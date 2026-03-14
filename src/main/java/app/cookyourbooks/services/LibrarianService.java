package app.cookyourbooks.services;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import app.cookyourbooks.conversion.ConversionRegistry;
import app.cookyourbooks.conversion.ConversionRule;
import app.cookyourbooks.conversion.ConversionRulePriority;
import app.cookyourbooks.conversion.LayeredConversionRegistry;
import app.cookyourbooks.model.PersonalCollectionImpl;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.repository.RecipeCollectionRepository;

public class LibrarianService {

  private final RecipeCollectionRepository collectionRepo;
  private ConversionRegistry conversionRegistry;

  public LibrarianService(
      RecipeCollectionRepository collectionRepo, ConversionRegistry conversionRegistry) {
    this.collectionRepo = Objects.requireNonNull(collectionRepo, "collectionRepo must not be null");
    this.conversionRegistry =
        Objects.requireNonNull(conversionRegistry, "conversionRegistry must not be null");
  }

  /** Returns all collections from the collection repository. */
  public List<RecipeCollection> getCollections() {
    return collectionRepo.findAll();
  }

  /** Creates and persists a new personal collection with the given name. */
  public RecipeCollection createCollection(String name) {
    RecipeCollection collection = PersonalCollectionImpl.builder().title(name).build();
    collectionRepo.save(collection);
    return collection;
  }

  /** Finds a collection by title and returns its recipes. */
  public List<Recipe> getRecipesInCollection(String title) {
    RecipeCollection collection =
        collectionRepo.findByTitle(title).orElseThrow(() -> new CollectionNotFoundException(title));
    return collection.getRecipes();
  }

  /** Returns house conversion rules currently present in the conversion registry. */
  public List<ConversionRule> getConversions() {
    return extractHouseRules(conversionRegistry);
  }

  /** Adds a new house conversion rule. */
  public void addConversion(ConversionRule rule) {
    conversionRegistry = conversionRegistry.withRule(rule, ConversionRulePriority.HOUSE);
  }

  /** Removes a house conversion rule by identifier ("fromUnit ingredient"), case-insensitive. */
  public boolean removeConversion(String ruleId) {
    String normalized = normalizeRuleId(ruleId);
    List<ConversionRule> house = extractHouseRules(conversionRegistry);
    boolean removed = house.removeIf(rule -> ruleIdentifier(rule).equals(normalized));
    if (!removed) {
      return false;
    }
    conversionRegistry =
        rebuildWithoutHouse(conversionRegistry).withRules(house, ConversionRulePriority.HOUSE);
    return true;
  }

  /** Returns the current registry instance after any add/remove updates. */
  public ConversionRegistry getConversionRegistry() {
    return conversionRegistry;
  }

  private static String normalizeRuleId(String ruleId) {
    return ruleId == null ? "" : ruleId.trim().toLowerCase(Locale.ROOT);
  }

  private static String ruleIdentifier(ConversionRule rule) {
    String ingredient =
        rule.ingredientName() == null
            ? "any"
            : rule.ingredientName().trim().toLowerCase(Locale.ROOT);
    return rule.fromUnit().getAbbreviation().toLowerCase(Locale.ROOT) + " " + ingredient;
  }

  private static ConversionRegistry rebuildWithoutHouse(ConversionRegistry registry) {
    if (!(registry instanceof LayeredConversionRegistry layered)) {
      throw new IllegalStateException("Expected LayeredConversionRegistry implementation");
    }

    Map<ConversionRulePriority, List<ConversionRule>> snapshot = extractRulesByPriority(layered);
    ConversionRegistry rebuilt = new LayeredConversionRegistry();
    for (ConversionRulePriority priority : ConversionRulePriority.values()) {
      if (priority == ConversionRulePriority.HOUSE) {
        continue;
      }
      rebuilt = rebuilt.withRules(snapshot.getOrDefault(priority, List.of()), priority);
    }
    return rebuilt;
  }

  private static List<ConversionRule> extractHouseRules(ConversionRegistry registry) {
    if (!(registry instanceof LayeredConversionRegistry layered)) {
      return List.of();
    }
    Map<ConversionRulePriority, List<ConversionRule>> all = extractRulesByPriority(layered);
    return new ArrayList<>(all.getOrDefault(ConversionRulePriority.HOUSE, List.of()));
  }

  @SuppressWarnings("unchecked")
  private static Map<ConversionRulePriority, List<ConversionRule>> extractRulesByPriority(
      LayeredConversionRegistry layered) {
    try {
      Field rulesField = LayeredConversionRegistry.class.getDeclaredField("rules");
      rulesField.setAccessible(true);
      EnumMap<ConversionRulePriority, List<ConversionRule>> rules =
          (EnumMap<ConversionRulePriority, List<ConversionRule>>) rulesField.get(layered);

      EnumMap<ConversionRulePriority, List<ConversionRule>> copy =
          new EnumMap<>(ConversionRulePriority.class);
      for (ConversionRulePriority priority : ConversionRulePriority.values()) {
        copy.put(priority, new ArrayList<>(rules.getOrDefault(priority, List.of())));
      }
      return copy;
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to inspect layered conversion registry", e);
    }
  }
}

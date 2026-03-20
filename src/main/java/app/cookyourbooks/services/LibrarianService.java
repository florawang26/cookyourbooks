package app.cookyourbooks.services;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.conversion.ConversionRegistry;
import app.cookyourbooks.conversion.ConversionRule;
import app.cookyourbooks.conversion.ConversionRulePriority;
import app.cookyourbooks.conversion.LayeredConversionRegistry;
import app.cookyourbooks.model.PersonalCollectionImpl;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.repository.RecipeCollectionRepository;
import app.cookyourbooks.repository.RecipeRepository;

public class LibrarianService {

  private final RecipeCollectionRepository collectionRepo;
  private final @Nullable RecipeRepository recipeRepo;
  private ConversionRegistry conversionRegistry;

  public LibrarianService(
      RecipeCollectionRepository collectionRepo, ConversionRegistry conversionRegistry) {
    this(collectionRepo, null, conversionRegistry);
  }

  public LibrarianService(
      RecipeCollectionRepository collectionRepo,
      @Nullable RecipeRepository recipeRepo,
      ConversionRegistry conversionRegistry) {
    this.collectionRepo = Objects.requireNonNull(collectionRepo, "collectionRepo must not be null");
    this.recipeRepo = recipeRepo;
    this.conversionRegistry =
        Objects.requireNonNull(conversionRegistry, "conversionRegistry must not be null");
  }

  /** Returns all collections from the collection repository. */
  public List<RecipeCollection> getCollections() {
    return collectionRepo.findAll();
  }

  /** Returns all recipes available in the repository. */
  public List<Recipe> getAllRecipes() {
    if (recipeRepo != null) {
      return recipeRepo.findAll();
    }

    return collectionRepo.findAll().stream()
        .flatMap(c -> c.getRecipes().stream())
        .distinct()
        .toList();
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

  /** Returns all collection titles, used by tab completion and command validation. */
  public List<String> getCollectionTitles() {
    return getCollections().stream().map(RecipeCollection::getTitle).toList();
  }

  /**
   * Finds recipes matching CLI lookup rules.
   *
   * <p>If query length is 3+, first try short-id prefix matches. If none match, fall back to
   * case-insensitive title substring matching. If query length is under 3, only title matching is
   * used.
   */
  public List<Recipe> findRecipes(String query) {
    String trimmed = query == null ? "" : query.trim();
    if (trimmed.isBlank()) {
      return List.of();
    }

    List<Recipe> all = getAllRecipes();
    String lower = trimmed.toLowerCase(Locale.ROOT);

    if (trimmed.length() >= 3) {
      List<Recipe> byId =
          all.stream()
              .filter(r -> r.getId() != null)
              .filter(r -> r.getId().toLowerCase(Locale.ROOT).startsWith(lower))
              .toList();
      if (!byId.isEmpty()) {
        return byId;
      }
    }

    return all.stream().filter(r -> r.getTitle().toLowerCase(Locale.ROOT).contains(lower)).toList();
  }

  /** Returns the first collection title that currently contains the recipe, if any. */
  public Optional<String> findCollectionTitleForRecipe(String recipeId) {
    return getCollections().stream()
        .filter(c -> c.containsRecipe(recipeId))
        .map(RecipeCollection::getTitle)
        .findFirst();
  }

  /** Searches recipes by ingredient-name substring and includes collection context for display. */
  public List<RecipeSearchHit> searchByIngredient(String ingredientQuery) {
    String trimmed = ingredientQuery == null ? "" : ingredientQuery.trim();
    if (trimmed.isBlank()) {
      return List.of();
    }

    String lower = trimmed.toLowerCase(Locale.ROOT);
    List<RecipeSearchHit> results = new ArrayList<>();
    for (Recipe recipe : getAllRecipes()) {
      boolean contains =
          recipe.getIngredients().stream()
              .anyMatch(i -> i.getName().toLowerCase(Locale.ROOT).contains(lower));
      if (!contains) {
        continue;
      }
      String collection = findCollectionTitleForRecipe(recipe.getId()).orElse("Unknown Collection");
      results.add(new RecipeSearchHit(recipe, collection));
    }
    return results;
  }

  /** Deletes a recipe from repository and all collections that include it. */
  public void deleteRecipe(Recipe recipe) {
    if (recipeRepo == null) {
      throw new IllegalStateException("Recipe repository is not available for deletion");
    }
    recipeRepo.delete(recipe.getId());
  }

  /**
   * Saves a derived recipe to repository and to one collection that contains the original recipe.
   */
  public void saveDerivedRecipe(Recipe original, Recipe derived) {
    if (recipeRepo == null) {
      throw new IllegalStateException("Recipe repository is not available for save");
    }

    recipeRepo.save(derived);

    RecipeCollection targetCollection =
        getCollections().stream()
            .filter(c -> c.containsRecipe(original.getId()))
            .findFirst()
            .orElse(null);
    if (targetCollection != null) {
      collectionRepo.save(targetCollection.addRecipe(derived));
      return;
    }

    // Fallback for edge cases: place the derived recipe into the first available collection.
    getCollections().stream().findFirst().ifPresent(c -> collectionRepo.save(c.addRecipe(derived)));
  }

  /** Returns recipe titles and short IDs for completion. */
  public List<RecipeIdentity> getRecipeIdentities() {
    return getAllRecipes().stream().map(r -> new RecipeIdentity(r.getTitle(), shortId(r))).toList();
  }

  /** Returns house conversion rules currently present in the conversion registry. */
  public List<ConversionRule> getConversions() {
    return extractHouseRules(conversionRegistry);
  }

  /** Returns conversion identifiers like "cup flour" and "tbsp any". */
  public List<String> getConversionIdentifiers() {
    return getConversions().stream().map(LibrarianService::ruleIdentifier).toList();
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

  private static String shortId(Recipe recipe) {
    String id = recipe.getId();
    if (id == null) {
      return "";
    }
    return id.length() <= 8 ? id : id.substring(0, 8);
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

  public record RecipeSearchHit(Recipe recipe, String collectionTitle) {}

  public record RecipeIdentity(String title, String shortId) {}
}

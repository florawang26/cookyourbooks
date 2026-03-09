# Reflection (A5: Interactive CLI)

Complete all 6 questions below. Reflection questions are worth 20 points total (see grading rubric).

---

1. **Applying Boundary Heuristics:** Which of the four L18 heuristics (rate of change, actor,
   interface segregation, testability) most influenced your service layer design? Give a concrete
   example: describe a specific boundary you drew (or chose not to draw) and explain which
   heuristic(s) informed that decision. How did the three actors (Librarian, Cook, Planner)
   influence your thinking? If multiple heuristics pointed in different directions, how did you
   resolve the tension?

Answer:

---

2. **ADR Writing Experience:** Reflect on writing your ADRs. Did documenting your decisions change
   how you thought about them? Was there a moment where writing the "Consequences" section made you
   reconsider a choice? How useful do you think ADRs would be on a team project vs. a solo
   assignment?

Answer:

---

3. **Transformation vs. Persistence:** A4's `RecipeService.scaleRecipe()` always saved. Your design
   needed to support "preview before save." Describe concretely how your service layer handles this
   differently. What methods exist? How does the CLI compose them? What would break if you tried to
   bolt this capability onto A4's interface?

Answer:

---

4. **Cook Mode State Management:** Interactive cooking mode tracks state (current step, original
   recipe). Where does this state live in your architecture — in the CLI controller, in a service,
   in a dedicated session object? What tradeoffs did you consider? The Cook actor has different
   needs than the Librarian — how did this influence where you placed cook mode state? Could the
   same state management approach work for a future "meal planning session" for the Planner actor?

Answer:

---

5. **E2E Testing Experience:** This assignment used E2E tests with a dumb terminal instead of mocks.
   Compare this to A4's mock-based approach. Which bugs did E2E testing catch (or would catch) that
   mocks might miss? Were there situations where you wished you had finer-grained unit tests? What's
   your takeaway about when to use each approach?

Answer:

---

6. **AI Collaboration:** Which parts of the CLI did AI help you build most effectively? Where did
   you need to think independently about design? Did AI help or hinder your architectural thinking
   — for example, did it suggest designs that violated the boundary heuristics, or did it help you
   apply them?

Answer:

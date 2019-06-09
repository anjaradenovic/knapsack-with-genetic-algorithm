import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

public class Main {

  private static final List<Solution> solutions = new ArrayList<>();
  private static final int MAX_WEIGHT = 150;
  private static final int POPULATION_SIZE = 100;
  private static Solution bestSolutionSoFar = null;
  private static final double MUTATION_PROBABILITY = 0.05;
  private static List<Item> items;
  private static int counterSinceLastNewBestSolution = 0;
  private static final int MAX_INTERATIONS_WITHOUT_PROGRESS = 2000;
  // mapa broja iteracije u kojoj je pronadjeno (do tada) najbolje resenje i to resenje
  // npr.
  // {
  //   23 : 537,
  //   45 : 580
  //   ...
  // }
  private static final Map<Integer, Double> iterationToFitnessMap = new LinkedHashMap<>();
  private static int totalNumberOfIterations = 0;

  public static void main(String[] args) throws IOException {

    items = getItemsFromFile("items.txt");

    generatePopulation();

    while (counterSinceLastNewBestSolution < MAX_INTERATIONS_WITHOUT_PROGRESS) {
      totalNumberOfIterations++;
      evaluate();
      chooseSurvivors();
      crossoverAndMutate();
    }

    System.out.println("Najbolje resenje ima fitness " + fitness(bestSolutionSoFar));
    createChart();
  }

  private static void generatePopulation() {

    while (solutions.size() < POPULATION_SIZE) {
      Solution solution = generateSolution(items);

      if (!solutionExists(solution)) {
        solutions.add(solution);
      }
    }

  }

  private static boolean solutionExists(Solution newSolution) {

    for (Solution solution : solutions) {
      if (areEqual(solution, newSolution)) {
        return true;
      }
    }

    return false;
  }

  private static void chooseSurvivors() {

    List<Solution> bestHalf = Main.solutions
        .stream()
        .sorted(Comparator.comparingDouble(Main::fitness))
        .collect(Collectors.toList())
        .subList(solutions.size() / 2, Main.solutions.size());

    solutions.clear();
    solutions.addAll(bestHalf);
  }

  private static Solution mutate(Solution firstSolution) {

    List<Integer> indexes = new ArrayList<>(firstSolution.getChosenIndexes());

    Random random = new Random();

    for (int i = 0; i < indexes.size(); i++) {
      if (random.nextDouble() <= MUTATION_PROBABILITY) {
        indexes.set(i, random.nextInt(items.size()));
      }
    }

    return new Solution(indexes, items);
  }

  private static void crossoverAndMutate() {

    List<Solution> createdSolutions = new ArrayList<>();
    Random random = new Random();

    while (createdSolutions.size() < POPULATION_SIZE / 2) {

      int firstIndex = random.nextInt(POPULATION_SIZE / 2);
      int secondIndex = 0;

      do {
        secondIndex = random.nextInt(POPULATION_SIZE / 2);
      } while (firstIndex == secondIndex);


      Solution solution1 = solutions.get(firstIndex);
      Solution solution2 = solutions.get(secondIndex);

      Pair<Solution> solutionPair = crossover(solution1, solution2);

      Solution firstSolution = mutate(solutionPair.first);
      Solution secondSolution = mutate(solutionPair.second);

      if (createdSolutions.size() < POPULATION_SIZE / 2) {
        if (firstSolution.weight() <= MAX_WEIGHT) {
          if (isValid(firstSolution)) {
            if (!solutionExists(firstSolution)) {
              createdSolutions.add(firstSolution);
              solutions.add(firstSolution);
            }
          }
        }
      }

      if (createdSolutions.size() < POPULATION_SIZE / 2) {
        if (secondSolution.weight() <= MAX_WEIGHT) {
          if (isValid(secondSolution)) {
            if (!solutionExists(secondSolution)) {
              createdSolutions.add(secondSolution);
              solutions.add(secondSolution);
            }
          }
        }
      }
    }

  }

  private static boolean isValid(Solution first) {

    return first.getChosenIndexes()
        .stream()
        .distinct()
        .collect(Collectors.toList())
        .size() == first.getChosenIndexes().size();
  }

  private static Pair<Solution> crossover(Solution solution1,
                                          Solution solution2) {

    Solution solution3;
    Solution solution4;

    List<Integer> indices3 = new LinkedList<>();

    for (int i = 0; i < solution1.getChosenIndexes().size() / 2; i++) {
      if (!indices3.contains(solution1.getChosenIndexes().get(i))) {
        indices3.add(solution1.getChosenIndexes().get(i));
      }
    }

    for (int i = solution2.getChosenIndexes().size() / 2; i < solution2.getChosenIndexes().size(); i++) {
      if (!indices3.contains(solution2.getChosenIndexes().get(i))) {
        indices3.add(solution2.getChosenIndexes().get(i));
      }
    }

    List<Integer> indices4 = new ArrayList<>();

    for (int i = 0; i < solution2.getChosenIndexes().size() / 2; i++) {
      if (!indices4.contains(solution2.getChosenIndexes().get(i))) {
        indices4.add(solution2.getChosenIndexes().get(i));
      }
    }

    for (int i = solution1.getChosenIndexes().size() / 2; i < solution1.getChosenIndexes().size(); i++) {
      if (!indices4.contains(solution1.getChosenIndexes().get(i))) {
        indices4.add(solution1.getChosenIndexes().get(i));
      }
    }

    solution3 = new Solution(new ArrayList<>(indices3), items);
    solution4 = new Solution(new ArrayList<>(indices4), items);

    return new Pair<>(solution3, solution4);
  }

  private static void evaluate() {

    List<Solution> list = new ArrayList<>(solutions);
    list.sort(Comparator.comparingDouble(new ToDoubleFunction<Solution>() {
      @Override public double applyAsDouble(Solution solution1) {
        return fitness(solution1);
      }
    }));
    Solution newBest = list.get(solutions.size() - 1);

    if (bestSolutionSoFar == null || fitness(newBest) > fitness(bestSolutionSoFar)) {
      System.out.println("Pronadjeno je resenje " + newBest + " koje je bolje od trenutnog najboljeg " + bestSolutionSoFar);
      iterationToFitnessMap.put(totalNumberOfIterations, fitness(newBest));
      counterSinceLastNewBestSolution = 0;
      bestSolutionSoFar = newBest;
    } else {
      counterSinceLastNewBestSolution++;
    }

  }

  private static double fitness(Solution solution) {
    return (double) solution.value() + (double) solution.value() / (double) solution.weight();
  }

  private static List<Item> getItemsFromFile(String filePath) throws IOException {
    List<String> strings = Files.readAllLines(Paths.get(filePath));
    List<Item> items = new ArrayList<>();
    for (String s : strings) {
      if (!s.isEmpty()) {
        int value = Integer.parseInt(s.split(",")[0]);
        int weight = Integer.parseInt(s.split(",")[1]);
        items.add(new Item(weight, value));
      }
    }
    return items;
  }

  private static Solution generateSolution(List<Item> originalItems) {

    Random random = new Random();

    int currentWeight = 0;

    List<Integer> chosenIndexes = new ArrayList<>();

    List<Integer> availableIndexes = new ArrayList<>();

    for (int i = 0; i < originalItems.size(); i++) {
      availableIndexes.add(i);
    }

    while (currentWeight < MAX_WEIGHT && availableIndexes.size() > 0) {

      int newIndex = availableIndexes.get(random.nextInt(availableIndexes.size()));

      if (currentWeight + originalItems.get(newIndex).getWeight() <= MAX_WEIGHT) {
        chosenIndexes.add(newIndex);
        currentWeight += originalItems.get(newIndex).getWeight();
      }

      availableIndexes.removeIf(integer -> integer == newIndex);
    }

    return new Solution(chosenIndexes, originalItems);
  }

  static class Pair<A> {

    private A first;

    private A second;
    public Pair(A first, A second) {
      this.first = first;
      this.second = second;
    }

    public A getFirst() {
      return first;
    }

    public A getSecond() {
      return second;
    }

  }
  private static boolean areEqual(Solution solution1, Solution solution2) {

    List<Integer> indices1 = new ArrayList<>(solution1.getChosenIndexes());
    List<Integer> indices2 = new ArrayList<>(solution2.getChosenIndexes());

    if (indices1.size() != indices2.size()) {
      return false;
    }

    Collections.sort(indices1);
    Collections.sort(indices2);

    for (int i = 0; i < indices1.size(); i++) {
      if (!indices1.get(i).equals(indices2.get(i))) {
        return false;
      }
    }

    return true;
  }

  private static String readFile(String path) throws IOException {

    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, StandardCharsets.UTF_8);
  }

  private static void createChart() throws IOException {
    List<Integer> iterationsList = new ArrayList<>(iterationToFitnessMap.keySet());

    List<Double> fitnessList = new ArrayList<>();

    for (Integer iteration : iterationsList) {
      fitnessList.add(iterationToFitnessMap.get(iteration));
    }

    String iterationValuesAsString = iterationsList.toString();
    String fitnessValuesAsString = fitnessList.toString();

    String templateFileContents = readFile("template/page-template.html");
    String newFileContents = templateFileContents
        .replace("labels-go-here", iterationValuesAsString)
        .replace("data-goes-here", fitnessValuesAsString)
        .replace("line1-goes-here", "Velicina populacije: " + POPULATION_SIZE)
        .replace("line2-goes-here", "Maksimalna tezina ranca: " + MAX_WEIGHT)
        .replace("line3-goes-here", "Verovatnoca za mutaciju: " + MUTATION_PROBABILITY)
        .replace("line4-goes-here", "Maksimalan broj iteracija bez napretka: " + MAX_INTERATIONS_WITHOUT_PROGRESS)
        .replace("line5-goes-here", "Najbolje resenje: " + bestSolutionSoFar + " - fitness = " + fitness(bestSolutionSoFar));


    Files.write(Paths.get("chart.html"), newFileContents.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
  }

}

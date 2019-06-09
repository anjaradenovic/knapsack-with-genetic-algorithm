import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Solution {

  private final List<Integer> chosenIndexes;
  private final List<Item> allItems;

  public Solution(List<Integer> chosenIndexes, List<Item> allItems) {
    this.chosenIndexes = chosenIndexes;
    this.allItems = allItems;
  }


  public int weight() {
    int sum = 0;
    for (Integer integer : chosenIndexes) {
      int weight = allItems.get(integer).getWeight();
      sum += weight;
    }
    return sum;
  }

  public int value() {
    int sum = 0;
    for (Integer integer : chosenIndexes) {
      int value = allItems.get(integer).getValue();
      sum += value;
    }
    return sum;
  }

  @Override
  public String toString() {

    return "value: " + value() + ", weight: " + weight() + ", " + new ArrayList<>(chosenIndexes);
  }

  public List<Integer> getChosenIndexes() {
    return chosenIndexes;
  }


  public static void main(String[] args) {
    Random random = new Random();

    try {
      FileOutputStream out = new FileOutputStream(new File("items.txt"));
      PrintWriter printWriter = new PrintWriter(out);
      for (int i = 0; i < 1000; i++) {
        int value = 1 + random.nextInt(100);
        int weight = 1 + random.nextInt(100);
        printWriter.println(value + "," + weight);
      }
      printWriter.flush();
      out.close();
      printWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


}

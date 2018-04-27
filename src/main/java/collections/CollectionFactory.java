package collections;

import java.util.*;
import java.util.stream.Collectors;

public class CollectionFactory {

    public static void main(String[] args) {
        List<Integer> integerList = List.of(5, 8, 1, 7, 6, 10 , 15);
        System.out.println(integerList);

        try {
            integerList.sort(Integer::compareTo);
            System.err.println("This shall not pass");
        } catch (UnsupportedOperationException e) {
            System.out.println("This list is IMMUTABLE");
        }




        Set<String> stringSet = Set.of("A", "B", "C");

        System.out.println(stringSet);
        try {
            stringSet.add("D");
            System.err.println("This shall not pass");
        } catch (UnsupportedOperationException e) {
            System.out.println("This set is IMMUTABLE");
        }



    }
}

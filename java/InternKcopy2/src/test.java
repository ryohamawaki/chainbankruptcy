import ChainBankruptcy.Constants;

import java.util.ArrayList;
import java.util.Random;

public class test {
    public static void main(String[] args) {
        Random rnd = new Random(1);
        for (int i = 0; i < 100; i++) {
            System.out.println(rnd.nextDouble());
        }
        int a = (int) (Constants.SmallN / Constants.LargeN);
        System.out.println(a);

        ArrayList<Integer> one_to__ten = new ArrayList<Integer>();
        for(int i = 10; i > 0; i--){
            one_to__ten.add(i);
        }
        for(int i = 0; i < 10; i++){
            System.out.println(one_to__ten.get(i));
        }

        one_to__ten.remove(one_to__ten.indexOf(8));

        for(int i = 0; i < 9; i++){
            System.out.println(one_to__ten.get(i));
        }

    }
}


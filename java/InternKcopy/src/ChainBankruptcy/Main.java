package ChainBankruptcy;
import ChainBankruptcy.Constants;

import javax.swing.plaf.synth.SynthLookAndFeel;
import java.net.IDN;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    public static void main(String[] args){
        for(int x = 0; x < Constants.Args.trial_num; x++){
            Random rand = new Random(x+1);
            double r_1 = rand.nextDouble();
            double sum_marketable_assets = (130 + (20 * r_1)) * Constants.N;
            ArrayList<Integer> numbers_rupt = new ArrayList<Integer>();



            ArrayList<Bank> banks = new ArrayList<Bank>();

            Bank.Initializing_Interbank_Network(banks, rand);

            ArrayList<MarketAsset> markets = MarketAsset.makeMarketAssets(rand);

            Bank.Initializing_BalanceSheet(banks, sum_marketable_assets, markets, rand);

            for(int t = 0; t <= Constants.time; t++){
                if(t == Constants.rupttime){
                    int ID = Constants.Args.start_index;
                    Bank.go_bankrupt(banks, ID);
                }
                Bank.buy_or_sell_marketable_assets(banks, markets, rand);

                MarketAsset.update_price(banks, markets, rand);

                Bank.update_BalanceSheet(banks, markets);

                Bank.go_eachBankrupt(banks, markets);

                for(int i = 0; i < Constants.N; i++){
                    for(int j = 0; j < banks.get(i).neighborOut.size(); j++){
                        System.out.println(i + "　→  " + banks.get(i).neighborOut.get(j));
                    }
                }

                for(int i = 0; i < Constants.N; i++){
                    for(int j = 0; j < banks.get(i).BalanceSheet.size(); j++){
                        System.out.println("BS(" + i +"," + j + "):" + banks.get(i).BalanceSheet.get(j) + "  ");
                    }
                    System.out.println();
                }

                int number_bankrupt = Bank.countrupt(banks);
                numbers_rupt.add(number_bankrupt);
            }

            System.out.println(numbers_rupt.get(0));
            for(int i = 0; i < 10; i++) {
                System.out.println(numbers_rupt.get(i+1) - numbers_rupt.get(i));
            }
        }
    }
}

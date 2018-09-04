package ChainBankruptcy;

import java.util.Random;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args){
        for(int x = 0; x < Constants.Args.trial_num; x++){
            Random rand = new Random(x+1);
            double r_1 = rand.nextDouble();
            double sum_marketable_assets = (130 + (20 * r_1)) * Constants.N;
            ArrayList<Integer> numbers_rupt = new ArrayList<Integer>();


            ArrayList<Bank> banks = Bank.InitializeInterbankNetwork(rand);
            //Bank.OutputNetwork(banks);
            ArrayList<MarketAsset> markets = MarketAsset.MakeMarketAssets(rand);

            BalanceSheet.InitializeBalanceSheet(banks, sum_marketable_assets, markets, rand);

            for(int t = 0; t <= Constants.time; t++){
                if(t == Constants.rupttime){
                    int ID = Constants.Args.start_index;
                    if(banks.get(ID).status) {
                        Bank.GoBankrupt(banks, ID);
                    }
                }
                Bank.BuyOrSellMarketableAssets(banks, markets, rand);

                MarketAsset.UpdatePrice(banks, markets, rand);

                BalanceSheet.UpdateBalanceSheet(banks, markets);

                Bank.GoEachBankrupt(banks, markets);
            }
            int number_bankrupt = Bank.countrupt(banks);
            numbers_rupt.add(number_bankrupt);

            System.out.println(numbers_rupt.get(0));
        }
    }
}

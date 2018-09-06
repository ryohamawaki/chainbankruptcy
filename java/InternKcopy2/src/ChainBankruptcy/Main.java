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
            ArrayList<MarketAsset> markets = MarketAsset.MakeMarketAssets(rand);

            Bank.InitializeFinancing(banks, sum_marketable_assets, rand);
            for(Bank b: banks) { b.InitializeBalanceSheet(banks, sum_marketable_assets, markets, rand); }

            BalanceSheet.OutputBalanceSheet(banks, markets);

            for(int t = 0; t <= Constants.time; t++){
                if(t == Constants.rupttime){
                    int ID = Constants.Args.start_index;
                    if(banks.get(ID).status) {
                        Bank.GoBankrupt(banks, ID);
                    }
                }
                Bank.BuyOrSellMarketableAssets(banks, markets, rand);

                MarketAsset.UpdatePrice(banks, markets, rand);

                for(Bank b: banks){ b.UpdateBalanceSheet(markets);}

                // BalanceSheet.OutputBalanceSheet(banks, markets);

                Bank.GoEachBankrupt(banks, markets);
            }
            int number_bankrupt = Bank.countrupt(banks);
            numbers_rupt.add(number_bankrupt);

            System.out.println(numbers_rupt.get(0));
        }
    }
}

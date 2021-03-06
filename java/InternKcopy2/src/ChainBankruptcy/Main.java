package ChainBankruptcy;

import java.util.Random;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args){
        Constants.Args.substituteArgs(args);

        for(int x = 0; x < Constants.Args.trial_num; x++){
            Random rand = new Random(x+1);
            double r_1 = rand.nextDouble();
            double sum_marketable_assets = (130 + (20 * r_1)) * Constants.N;
            ArrayList<Integer> numbers_rupt = new ArrayList<Integer>();

            MarketAsset market = MarketAsset.MakeMarketAssets(rand);
            ArrayList<Bank> banks = Bank.InitializeInterbankNetwork(rand, sum_marketable_assets);

            for(Bank b: banks) { b.InitializeBalanceSheet(banks, sum_marketable_assets, market, rand); }


            for(int t = 0; t <= Constants.time; t++){
                if(t == Constants.rupttime){
                    int ID = Constants.Args.start_index;
                    if(banks.get(ID).status) {
                        Bank.GoBankrupt(banks, ID);
                    }
                }
                market.DealMarketAssets(banks, rand);
                market.UpdateFundamentalPrice(rand);
                for(Bank b: banks){ b.UpdateBalanceSheet(market.getLatestMarketPrice() );}
                double var = Bank.calculate_VaR(market);
                for(Bank b: banks) { TextFile.OutputBalanceSheet(b, var, t); }

                Bank.GoEachBankrupt(banks, market);
            }

            int num_bankrupted = 0;
            for(Bank b: banks) { if(!b.status) { num_bankrupted += 1; } }
            numbers_rupt.add(num_bankrupted);

            TextFile.OutputStatics(numbers_rupt.get(0));
        }
    }
}

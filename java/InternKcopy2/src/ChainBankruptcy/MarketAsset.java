package ChainBankruptcy;
import java.util.*;


public class MarketAsset {
    ArrayList<Double> fundamental_price = new ArrayList<Double>();
    ArrayList<Double> market_price = new ArrayList<Double>();

    public ArrayList<Double> getFundamental_price(){
        return this.fundamental_price;
    }
    public ArrayList<Double> getMarketPrice(){
        return this.market_price;
    }
    public static MarketAsset MakeMarketAssets(Random rand){
        MarketAsset ma = new MarketAsset();
        ArrayList<Double> price = new ArrayList<Double>();			//過去(m+1)日間における理論価格（＝市場価格）
        double p = 100.0;
        price.add(p);
        for(int i = 1; i < Constants.VaR.m+1;i++){
            p = p + Constants.VaR.r_f * p * Constants.VaR.delta_t + Constants.VaR.sigma * p * rand.nextGaussian() * Math.sqrt(Constants.VaR.delta_t);	//確率差分方程式で理論価格を計算
            price.add(p);								//第i試行の過去(m+1)日間の価格を加えていく
        }

        //市場価格の作成
        ArrayList<Double> market_price = new ArrayList<Double>();
        for(int i = 0; i < price.size(); i++){
            market_price.add(price.get(i));
        }

        ma.fundamental_price = price;
        ma.market_price = market_price;
        return ma;
    }

    public void DealMarketAssets(ArrayList<Bank> banks, Random rand){
        ArrayList<Integer> buyers  = new ArrayList<Integer>();		//買いの銀行のIDを格納
        ArrayList<Integer> sellers = new ArrayList<Integer>();		//売りの銀行のIDを格納

        for(int i = 0; i < Constants.N; i++){
            int b_or_s = banks.get(i).BuyOrSell(this, rand); // [TODO] 各銘柄に対して判定を行う必要があるはず
            if(b_or_s == 1){
                buyers.add(i);
            }else if(b_or_s == -1){
                sellers.add(i);
            }
        }
        while(buyers.size() > 0 && sellers.size() > 0) {
            double latest_price = getLatestMarketPrice();        //市場価格を取得
            int i = sellers.get(0);
            Bank seller = banks.get(i);
            seller.bs.num_stocks--;
            seller.bs.cash += latest_price / Constants.VaR.stockmulti;    //現金が増える
            int r = rand.nextInt(buyers.size());
            Bank buyer = banks.get(buyers.get(r));
            buyer.bs.num_stocks++;
            buyer.bs.cash -= latest_price / Constants.VaR.stockmulti;
            sellers.remove(0);
            buyers.remove(r);
        }

        //総株数を数える
        int total_num_stocks = 0;
        for(int i = 0; i < Constants.N; i++) {
            total_num_stocks += banks.get(i).bs.num_stocks;
        }

        UpdateMarketPrice(buyers.size() - sellers.size(), total_num_stocks);
    }

    private void UpdateMarketPrice(int buysurplus, int num_stocks) {
        ArrayList<Double> market_price = getMarketPrice();		//市場価格を取得
        double latest_mp = getLatestMarketPrice();

        double new_price = latest_mp + Constants.Args.coefficient_price_fluctuation * latest_mp * buysurplus / num_stocks;		//(Pn+1 - Pn) / Pn = α×(Nb -Ns)/[総株数]の計算
        market_price.add(new_price);

    }

    private double getLatestMarketPrice() {
        return market_price.get(market_price.size()-1);
    }

    public void UpdateFundamentalPrice(Random rand){
        double latest_price = fundamental_price.get(fundamental_price.size()-1);
        double new_price = latest_price + Constants.VaR.r_f * latest_price * Constants.VaR.delta_t + Constants.VaR.sigma * latest_price * rand.nextGaussian() * Math.sqrt(Constants.VaR.delta_t);	//確率差分方程式で理論価格を計算
        fundamental_price.add(new_price);
    }

    public static void OutputMarketPrice(ArrayList<MarketAsset> markets){
        System.out.println(markets.get(0).getMarketPrice());
    }

}

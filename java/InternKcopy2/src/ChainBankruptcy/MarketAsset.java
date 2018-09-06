package ChainBankruptcy;
import java.util.*;
import java.util.stream.Collectors;


public class MarketAsset {
    ArrayList<Double> price = new ArrayList<Double>();
    ArrayList<Double> marketprice = new ArrayList<Double>();
    double return_average;
    double return_sigma;

    public void setPrice(ArrayList<Double> newPrice){
        this.price = newPrice;
    }
    public void setMarketPrice(ArrayList<Double> newPrice){
        this.marketprice = newPrice;
    }
    public void setR_avg(double newR_avg){
        this.return_average = newR_avg;
    }
    public void setSigma_m(double newSigma_m){
        this.return_sigma = newSigma_m;
    }

    public ArrayList<Double> getPrice(){
        return this.price;
    }
    public ArrayList<Double> getMarketPrice(){
        return this.marketprice;
    }
    public static MarketAsset MakeMarketAssets(Random rand){
        return NewMarketAsset(rand);
    }

    private static MarketAsset NewMarketAsset(Random rand){
        MarketAsset ma = new MarketAsset();
        ArrayList<Double> price = new ArrayList<Double>();			//過去(m+1)日間における理論価格（＝市場価格）
        double p = 100.0;
        price.add(p);
        for(int i = 1; i < Constants.VaR.m+1;i++){
            p = p + Constants.VaR.r_f * p * Constants.VaR.delta_t + Constants.VaR.sigma * p * rand.nextGaussian() * Math.sqrt(Constants.VaR.delta_t);	//確率差分方程式で理論価格を計算
            price.add(p);								//第i試行の過去(m+1)日間の価格を加えていく
        }

        //市場価格の作成
        boolean start = false;					//市場価格を計算し始める
        ArrayList<Double> MarketPrice = new ArrayList<Double>();
        if(!start){
            for(int i = 0; i < price.size(); i++){
                MarketPrice.add(price.get(i));
            }
        }else{

        }

        //価格の初期値
        double firstPrice = price.get(Constants.VaR.m);							//t=0における価格（ｔ＝0における時価）を外部資産の価格の初期値とする。
        //平均リターン
        double r_avg = 0.0;	//平均リターン
        int num1 = 0;	//カウンター
        for(int k = 0; k < Constants.VaR.m; k++){
            if(price.get(k)==0.0) continue;							//例外処理。価格が0はスルー。
            r_avg += (price.get(k+1)/price.get(k));					//過去m日間の日次リターンを足しあわせていき、
            num1++;
        }
        if(num1==0){
            r_avg = 0.0;	//価格が0ばかりのときは、平均リターンも0。
        }else{
            r_avg = r_avg/num1;	//その平均を求める。
        }
        //標準偏差
        double sigma_m = 0.0;	//標準偏差
        int num2 = 0;	//カウンター
        for(int k=0; k < Constants.VaR.m; k++){
            if(price.get(k)==0.0) continue;	//例外処理。価格が0はスルー。
            sigma_m += ((price.get(k+1)/price.get(k))-r_avg)*((price.get(k+1)/price.get(k))-r_avg);	//過去m日間の平均リターン周りの二次モーメントを足しあわせていき、
            num2++;
        }
        if(num2==0){
            sigma_m = 0.0; //例外処理。価格0ばかりのときは、標準偏差も0とする。
        }else{
            sigma_m = Math.sqrt(sigma_m/num2);		//その平均（分散）の平方根を求める
        }
        //ma.setFirstPrice(firstPrice);
        ma.setPrice(price);
        ma.setMarketPrice(MarketPrice);
        ma.setR_avg(r_avg);
        ma.setSigma_m(sigma_m);
        return ma;
    }

    public static void deal_marketable_assets(ArrayList<Bank> banks, MarketAsset market, Random rand){
        ArrayList<Integer> buyers  = new ArrayList<Integer>();		//買いの銀行のIDを格納
        ArrayList<Integer> sellers = new ArrayList<Integer>();		//売りの銀行のIDを格納

        for(int i = 0; i < Constants.N; i++){
            int b_or_s = banks.get(i).BuyOrSell(market, rand); // [TODO] 各銘柄に対して判定を行う必要があるはず
            if(b_or_s == 1){
                buyers.add(i);
            }else if(b_or_s == -1){
                sellers.add(i);
            }
        }
        while(buyers.size() > 0 && sellers.size() > 0) {
            double latest_price = market.getLatestMarketPrice();        //市場価格を取得
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

        UpdateMarketPrice(buyers.size() - sellers.size(), total_num_stocks, market);
    }

    private static void UpdateMarketPrice(int buysurplus, int num_stocks, MarketAsset market) {
        ArrayList<Double> market_price = market.getMarketPrice();		//市場価格を取得
        double latest_mp = market.getLatestMarketPrice();

        double new_price = latest_mp + Constants.Args.coefficient_price_fluctuation * latest_mp * buysurplus / num_stocks;		//(Pn+1 - Pn) / Pn = α×(Nb -Ns)/[総株数]の計算
        market_price.add(new_price);

    }

    private double getLatestMarketPrice() {
        return marketprice.get(marketprice.size()-1);
    }

    public static void update_fundamental_price(MarketAsset market, Random rand){
        ArrayList<Double> fundamentalprice = market.getPrice();
        double latest_price = fundamentalprice.get(fundamentalprice.size()-1);
        double new_price = latest_price + Constants.VaR.r_f * latest_price * Constants.VaR.delta_t + Constants.VaR.sigma * latest_price * rand.nextGaussian() * Math.sqrt(Constants.VaR.delta_t);	//確率差分方程式で理論価格を計算
        fundamentalprice.add(new_price);
    }

    public static void OutputMarketPrice(ArrayList<MarketAsset> markets){
        System.out.println(markets.get(0).getMarketPrice());
    }

}

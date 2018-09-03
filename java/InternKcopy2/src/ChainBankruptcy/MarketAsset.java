package ChainBankruptcy;
import java.util.*;


public class MarketAsset {
    double firstprice;
    ArrayList<Double> price = new ArrayList<Double>();
    ArrayList<Double> marketprice = new ArrayList<Double>();
    double return_average;
    double return_sigma;
    double utility;

    public void setFirstprice(double newfirstprice) {
        this.firstprice = newfirstprice;
    }
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



    public double getFirstPrice(){
        return this.firstprice;
    }
    public ArrayList<Double> getPrice(){
        return this.price;
    }
    public ArrayList<Double> getMarketPrice(){
        return this.marketprice;
    }
    public double getR_avg(){
        return this.return_average;
    }
    public double getSigma_m(){
        return this.return_sigma;
    }

    public static ArrayList<MarketAsset> makeMarketAssets(Random rand){
        ArrayList<MarketAsset> marketAssets = new ArrayList<MarketAsset>();
        for(int j=0;j < Constants.VaR.M; j++){
            MarketAsset marketAsset = new MarketAsset();
            setupMarketAsset(marketAsset, j, rand);
            marketAssets.add(marketAsset);
        }
        return marketAssets;
    }

    public static void setupMarketAsset(MarketAsset marketAsset, Integer number, Random rand){
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
        //marketAsset.setFirstPrice(firstPrice);
        marketAsset.setPrice(price);
        marketAsset.setMarketPrice(MarketPrice);
        marketAsset.setR_avg(r_avg);
        marketAsset.setSigma_m(sigma_m);
    }

    public static void deal_marketable_assets(ArrayList<Bank> banks, ArrayList<MarketAsset> markets, Random rand){
        ArrayList<Double> market_price = markets.get(0).getMarketPrice();		//市場価格を取得
        ArrayList<Integer> buy_or_sell = Bank.Buy_or_Sell(banks);					//買いか売りかを取得
        double sum = 0.0;						//買いと売りどちらが多いかを判定
        ArrayList<Integer> plus= new ArrayList<Integer>();		//買いの銀行のIDを格納
        ArrayList<Integer> minus = new ArrayList<Integer>();		//売りの銀行のIDを格納

        for(int i = 0; i < Constants.N; i++){
            sum += buy_or_sell.get(i);
            if(buy_or_sell.get(i) == 1){
                plus.add(i);
            }else if(buy_or_sell.get(i) == -1){
                minus.add(i);
            }
        }
        for(int i = 0; i < Constants.N; i++){
            //買い手の方が多い時
            if(sum > 0){
                if(buy_or_sell.get(i) == -1){
                    banks.get(i).BalanceSheet.set(8, banks.get(i).BalanceSheet.get(8) - 1);				//買い手が多かったら、売り手は絶対売れる
                    banks.get(i).BalanceSheet.set(9, banks.get(i).BalanceSheet.get(9) + market_price.get(market_price.size() - 1) / Constants.VaR.stockmulti);	//現金が増える


                    int buyer = rand.nextInt(plus.size());
                    banks.get(plus.get(buyer)).BalanceSheet.set(8, banks.get(i).BalanceSheet.get(8) + 1);					//購入
                    banks.get(plus.get(buyer)).BalanceSheet.set(9, banks.get(i).BalanceSheet.get(9) - market_price.get(market_price.size() - 1) / Constants.VaR.stockmulti);	//現金が増える

                    plus.remove(plus.get(buyer));			//一度買ったら除外
                }
            }//売り手の方が多い時
            else{
                if(buy_or_sell.get(i) == 1){
                    banks.get(i).BalanceSheet.set(8, banks.get(i).BalanceSheet.get(8) + 1);				//買い手が多かったら、売り手は絶対売れる
                    banks.get(i).BalanceSheet.set(9, banks.get(i).BalanceSheet.get(9) - market_price.get(market_price.size() - 1) / Constants.VaR.stockmulti);	//現金が増える


                    //売り手は買い手の数だけしか売れない　→　ランダムにplusの中から取ってくる
                    int seller = rand.nextInt(minus.size());
                    banks.get(minus.get(seller)).BalanceSheet.set(8, banks.get(i).BalanceSheet.get(8) - 1);					//購入
                    banks.get(minus.get(seller)).BalanceSheet.set(9, banks.get(i).BalanceSheet.get(9) + market_price.get(market_price.size() - 1) / Constants.VaR.stockmulti);	//現金が増える

                    minus.remove(minus.get(seller));			//一度売ったら除外
                }
            }
        }

    }

    public static void update_price(ArrayList<Bank> banks, ArrayList<MarketAsset> markets, Random rand){
        update_fundamental_price(banks, markets, rand);
        update_market_price(banks, markets);
    }

    public static void update_market_price(ArrayList<Bank> banks, ArrayList<MarketAsset> markets){
        ArrayList<Double> marketprice = markets.get(0).getMarketPrice();		//市場価格を取得
        ArrayList<Integer> BORS = Bank.Buy_or_Sell(banks);					//買いか売りかを取得
        double buysurplus = 0.0;
        double number = 0.0;

        //買いがどれだけ多いかを数える
        for(int i = 0; i < Constants.N; i++){
            buysurplus += BORS.get(i);
        }

        //取引数を数える
        if(buysurplus > 0){
            number = (Constants.N - buysurplus) / 2;
        }else{
            number = (Constants.N + buysurplus) / 2;
        }

        //総株数を数える
        int sumofstock = 0;
        for(int i = 0; i < Constants.N; i++){
            sumofstock += banks.get(i).BalanceSheet.get(8);
        }

        double newprice = 0.0;					//新しい価格の初期化
        newprice = marketprice.get(marketprice.size()-1) + Constants.Args.coefficient_price_fluctuation * (marketprice.get(marketprice.size()-1)  * buysurplus / (sumofstock));		//(Pn+1 - Pn) / Pn = α×(Nb -Ns)/[総株数]の計算
        marketprice.add(newprice);
    }

    public static void update_fundamental_price(ArrayList<Bank> banks, ArrayList<MarketAsset> markets, Random rand){
        ArrayList<Double> fundamentalprice = markets.get(0).getPrice();
        double newprice = 0.0;
        newprice = fundamentalprice.get(fundamentalprice.size()-1) + Constants.VaR.r_f * fundamentalprice.get(fundamentalprice.size()-1) * Constants.VaR.delta_t + Constants.VaR.sigma * fundamentalprice.get(fundamentalprice.size()-1) * rand.nextGaussian() * Math.sqrt(Constants.VaR.delta_t);	//確率差分方程式で理論価格を計算
        fundamentalprice.add(newprice);
    }

}

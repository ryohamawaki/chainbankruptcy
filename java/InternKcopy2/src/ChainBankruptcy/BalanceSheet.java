package ChainBankruptcy;

import java.util.ArrayList;
import java.util.Random;

public class BalanceSheet {

    double asset_sum;        // 総資産
    double marketable_asset; // 市場性資産
    double borrowing_money;  // 借り入れ
    double equity_capital;   // 自己資本
    double account;          // 預金
    double lending_money;    // 貸し出し
    double gamma;            // 貸し出し比率
    double EquityCapitalRatio(ArrayList<Double> varlist) {
        double sum = 0.0;
        for (int i = 0; i < Constants.VaR.M; i++) {
            sum += (Math.abs(num_stocks[i]) * varlist.get(i));
        }
        return Constants.VaR.Control * equity_capital / sum;
    }
    int[] num_stocks = new int[Constants.VaR.M]; // 持ち株数
    double cash;             // 現金

    public static void InitializeBalanceSheet(ArrayList<Bank> banks, double sum_marketable_assets, ArrayList<MarketAsset> markets, Random rand){
        MakeOmega(banks, sum_marketable_assets, rand);
        MakeBalanceSheet(banks, sum_marketable_assets, markets, rand);
        MakeBorrowingAndLendingList(banks);
    }

    public static void MakeOmega(ArrayList<Bank> banks, double sum_marketable_assets, Random rand){
        for(int i = 0; i < Constants.N; i++){
            banks.get(i).sum_link_out = banks.get(i).neighborOut.size();
        }
        double r = rand.nextDouble();
        double sum_lending_money = (Constants.BalanceSheet.gamma_whole / (1.0 - Constants.BalanceSheet.gamma_whole)) * sum_marketable_assets;

        int Omega_denominator = 0;
        for(int i = 0; i < Constants.N; i++){
            for(int j = 0; j < banks.get(i).neighborOut.size(); j++){
                Omega_denominator += Math.pow(banks.get(i).sum_link_out * banks.get(banks.get(i).neighborOut.get(j)).sum_link_out, r);
            }
        }
        for(int i = 0; i < Constants.N; i++){
            for(int j = 0; j < banks.get(i).neighborOut.size(); j++){
                double omega = Math.pow(banks.get(i).sum_link_out * banks.get(banks.get(i).neighborOut.get(j)).sum_link_out, r) * sum_lending_money / Omega_denominator;
                banks.get(i).Omega.put(banks.get(i).neighborOut.get(j), omega);
            }
        }

    }

    public static void CountBorrowing_Money(){

    }


    public static void MakeBalanceSheet(ArrayList<Bank> banks, Double sum_marketable_assets, ArrayList<MarketAsset> markets, Random rand){
        double sum_lending_money = (Constants.BalanceSheet.gamma_whole / (1.0 - Constants.BalanceSheet.gamma_whole)) * sum_marketable_assets;
        ArrayList<Double> price_market = markets.get(0).getMarketPrice();

        /*for(int i = 0; i < Constants.N; i++){
            for(int j = 0; j < 10; j++){
                banks.get(i).BalanceSheet.add(0.0);
            }
        }*/

        double[] borrowing_money_count = new double[(int)Constants.N];
        for(int i = 0; i < Constants. N; i++){
            for(int j = 0; j < banks.get(i).neighborOut.size(); j++){
                int k = banks.get(i).neighborOut.get(j);
                borrowing_money_count[k] += banks.get(i).Omega.get(k);
            }
        }

        for(int i = 0; i < Constants. N; i++) {
            banks.get(i).bs = new BalanceSheet();
            banks.get(i).bs.borrowing_money = borrowing_money_count[i];

            double lending_money_count = 0.0;
            for (int j = 0; j < banks.get(i).neighborOut.size(); j++) {
                lending_money_count += banks.get(i).Omega.get(banks.get(i).neighborOut.get(j));
            }
            banks.get(i).bs.lending_money = lending_money_count;
        }

        for(int i = 0; i < Constants.N; i++) {
            double e = 0.0;
            double sum_borrowing_surplus = 0.0;
            double number_stock = 0.0;
            int number_stockInt = 0;

            for (int j = 0; j < Constants.N; j++) {
                sum_borrowing_surplus += Math.max(banks.get(i).bs.borrowing_money - banks.get(i).bs.lending_money, 0.0);
            }
            e = Math.max(banks.get(i).bs.borrowing_money - banks.get(i).bs.lending_money, 0.0) + (sum_marketable_assets - sum_borrowing_surplus) * (banks.get(i).bs.lending_money / sum_lending_money);
            number_stock = e * Constants.VaR.stockmulti / price_market.get(price_market.size() - 1);
            for (int j = 0; j < number_stock; j++) {
                number_stockInt++;
            }
            e = 0.0;
            e = price_market.get(price_market.size() - 1) * number_stockInt / Constants.VaR.stockmulti;

            banks.get(i).bs.marketable_asset = e;
            banks.get(i).bs.num_stocks[0] = number_stockInt;
        }
        for(int i = 0; i < Constants.N; i++) {
            double d = banks.get(i).neighborOut.size() * (30 + 10 * rand.nextDouble());
            banks.get(i).bs.account = d;
        }
        for (int i = 0; i < Constants.N; i++){
            double car = 0.1 + 0.2 * rand.nextDouble();
            double c = (banks.get(i).bs.account + banks.get(i).bs.borrowing_money) * (car / (1 - car));
            banks.get(i).bs.equity_capital = c;

            double a = Math.max(banks.get(i).bs.cash + banks.get(i).bs.marketable_asset + banks.get(i).bs.lending_money, banks.get(i).bs.equity_capital + banks.get(i).bs.account + banks.get(i).bs.borrowing_money);
            banks.get(i).bs.asset_sum = a;

            double gamma = banks.get(i).bs.lending_money / banks.get(i).bs.asset_sum;
            banks.get(i).bs.gamma = gamma;

            double money = banks.get(i).bs.equity_capital + banks.get(i).bs.account + banks.get(i).bs.borrowing_money - (banks.get(i).bs.marketable_asset + banks.get(i).bs.lending_money);
            banks.get(i).bs.cash = money;
        }
    }

    public static int CountUpNumStocks(ArrayList<Bank> banks, int id){
        int sum = 0;
        for(int i = 0; i < Constants.VaR.M; i++){
            sum += banks.get(id).bs.num_stocks[i];
        }
        return sum;
    }

    public static void MakeBorrowingAndLendingList(ArrayList<Bank> banks){
        for(int i = 0; i < Constants.N; i++){
            for(int j = 0; j < banks.get(i).neighborOut.size(); j++){
                int k = banks.get(i).neighborOut.get(j);
                double get = banks.get(i).Omega.get(k);
                banks.get(i).List_lending.put(k, get);
                banks.get(k).List_borrowing.put(i, get);
            }
        }

    }

    public static void UpdateBalanceSheet(ArrayList<Bank> banks, ArrayList<MarketAsset> markets){
        ArrayList<Double> marketprice = markets.get(0).getMarketPrice();

        for(int i = 0; i < Constants.N; i++){
            if(!banks.get(i).status){
                continue;
            }
            double e_update = 0.0;
            e_update = CountUpNumStocks(banks, i) * marketprice.get(marketprice.size() - 1) / Constants.VaR.stockmulti;
            banks.get(i).bs.marketable_asset = e_update;	//外部資産はBS(8)：持ち株数 * Mp（最新時刻）：市場価格から算出
        }

        for(int i = 0; i < Constants.N; i++){
            double l_update = 0.0;
            for(int j = 0; j < banks.get(i).neighborOut.size(); j++){
                l_update += banks.get(i).List_lending.get(banks.get(i).neighborOut.get(j));	//貸出額はLendListの総和から算出
            }
            banks.get(i).bs.lending_money = l_update;
        }

        for(int i = 0; i < Constants.N; i++){
            double b_update = 0.0;
            for(int j = 0; j < banks.get(i).neighborIn.size(); j++){
                b_update += banks.get(i).List_borrowing.get(banks.get(i).neighborIn.get(j));	//借入額はBorrowListの総和から算出
            }
            banks.get(i).bs.borrowing_money = b_update;
        }

        for(int i = 0; i < Constants.N; i++){
            double c_update = 0.0;
            double gap = -(banks.get(i).bs.cash + banks.get(i).bs.marketable_asset + banks.get(i).bs.lending_money
                    - banks.get(i).bs.equity_capital - banks.get(i).bs.account - banks.get(i).bs.borrowing_money);
            c_update = banks.get(i).bs.equity_capital - gap + 0.0001;   //0.0001は浮動小数点対策

            banks.get(i).bs.equity_capital = c_update;
        }

        for(int i = 0; i < Constants.N; i++){
            double a_update = 0.0;
            a_update = Math.max(banks.get(i).bs.cash + banks.get(i).bs.marketable_asset + banks.get(i).bs.lending_money,
                    banks.get(i).bs.equity_capital + banks.get(i).bs.account + banks.get(i).bs.borrowing_money);	//資産a=max(外部資産e+銀行間貸出l, 自己資本c+預金d+銀行間借入b)
            banks.get(i).bs.asset_sum = a_update;
        }
        update_Gap(banks);
    }

    public static void update_Gap(ArrayList<Bank> banks){
        for(int i = 0; i < Constants.N; i++){
            banks.get(i).gap = -(banks.get(i).bs.cash + banks.get(i).bs.marketable_asset + banks.get(i).bs.lending_money
                    - banks.get(i).bs.equity_capital - banks.get(i).bs.account - banks.get(i).bs.borrowing_money);
        }
    }

    public static void isClear(ArrayList<Bank> banks, int id){
        banks.get(id).bs.asset_sum = 0.0;
        banks.get(id).bs.marketable_asset = 0.0;
        banks.get(id).bs.borrowing_money = 0.0;
        banks.get(id).bs.equity_capital = 0.0;
        banks.get(id).bs.account = 0.0;
        banks.get(id).bs.lending_money = 0.0;
        banks.get(id).bs.gamma = 0.0;
        banks.get(id).bs.cash = 0.0;
    }
    public static void OutputBalanceSheet(ArrayList<Bank> banks){
        for(int i = 0; i < Constants.N; i++){
            System.out.println(i + "の総資産　　：" + banks.get(i).bs.asset_sum);
            System.out.println(i + "の市場性資産：" + banks.get(i).bs.marketable_asset);
            System.out.println(i + "の貸出     ：" + banks.get(i).bs.lending_money);
            System.out.println(i + "の現金　　　：" + banks.get(i).bs.cash);
            System.out.println(i + "の自己資本　：" + banks.get(i).bs.equity_capital);
            System.out.println(i + "の預金　　　：" + banks.get(i).bs.account);
            System.out.println(i + "の借入　　　：" + banks.get(i).bs.borrowing_money);
            System.out.println(i + "の持株数　　：" + banks.get(i).bs.num_stocks[0]);
            System.out.println();
        }
    }
}

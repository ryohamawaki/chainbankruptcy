package ChainBankruptcy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BalanceSheet {
    double asset_sum;        // 総資産

    double marketable_asset; // 市場性資産
    double borrowing_money;  // 借り入れ
    double equity_capital;   // 自己資本
    double account;          // 預金
    double lending_money;    // 貸し出し
    double gamma;            // 貸し出し比率
    double EquityCapitalRatio(double var) {
        double sum = Math.abs(num_stocks) * var;
        return Constants.VaR.Control * equity_capital / sum;
    }
    int num_stocks; // 持ち株数
    double cash;             // 現金


    public static ArrayList<Map<Integer, Double>> MakeOmega(ArrayList<ArrayList<Integer>> d_link_list, double sum_marketable_assets, Random rand){
        ArrayList<Map<Integer, Double>> omega = new ArrayList<>();
        for(int i = 0; i < Constants.N; i++){
            omega.add(new HashMap<Integer, Double>());
        }
        double r = rand.nextDouble();
        double sum_lending_money = (Constants.BalanceSheet.gamma_whole / (1.0 - Constants.BalanceSheet.gamma_whole)) * sum_marketable_assets;

        double Omega_denominator = 0.0;
        for(int i = 0; i < Constants.N; i++){
            ArrayList<Integer> neighbor_out = d_link_list.get(i);
            for(int n = 0; n < neighbor_out.size(); n++){
                int j = neighbor_out.get(n);
                int out_degree_j = d_link_list.get(j).size();
                Omega_denominator += Math.pow(neighbor_out.size() * out_degree_j, r);
            }
        }
        for(int i = 0; i < Constants.N; i++){
            ArrayList<Integer> neighbor_out = d_link_list.get(i);
            for(int n = 0; n < neighbor_out.size(); n++){
                int j = neighbor_out.get(n);
                int out_degree_j = d_link_list.get(j).size();
                double omega_i = Math.pow(neighbor_out.size() * out_degree_j, r) * sum_lending_money / Omega_denominator;
                omega.get(i).put(j, omega_i);
            }
        }
        return omega;
    }

    public static void MakeBorrowingAndLendingList(ArrayList<Bank> banks,ArrayList<Map<Integer, Double>> Omega){
        for(int i = 0; i < Constants. N; i++){
            Map<Integer,Double> lending_list = Omega.get(i);
            for(Map.Entry<Integer,Double> kv: lending_list.entrySet() ) {
                int j = kv.getKey();
                double amount = kv.getValue();
                Bank bi = banks.get(i);
                Bank bj = banks.get(j);
                bj.bs.borrowing_money += amount;
                bj.List_borrowing.put(i, amount);

                bi.bs.lending_money += amount;
                bi.List_lending.put(j, amount);
            }
        }
    }

    public void MakeBalanceSheet(int outDegree, Double sum_borrowing_surplus, Double sum_marketable_assets, MarketAsset market, Random rand){
        double sum_lending_money = (Constants.BalanceSheet.gamma_whole / (1.0 - Constants.BalanceSheet.gamma_whole)) * sum_marketable_assets;
        ArrayList<Double> price_market = market.getMarketPrice();

        double e_temp = Math.max(borrowing_money - lending_money, 0.0)
                + (sum_marketable_assets - sum_borrowing_surplus)
                * (lending_money / sum_lending_money);
        double number_stock = e_temp * Constants.VaR.stockmulti / price_market.get(price_market.size() - 1);
        int number_stockInt = (int) number_stock;
        double e = price_market.get(price_market.size() - 1) * number_stockInt / Constants.VaR.stockmulti;

        marketable_asset = e;
        num_stocks = number_stockInt;

        account = outDegree * (30 + 10 * rand.nextDouble());

        double car = Constants.Args.under_car + Constants.Args.width * rand.nextDouble();

        double sum = num_stocks * Bank.calculate_VaR(market);
        double c = car * (sum / Constants.VaR.Control);

        equity_capital = c;

        double a = Math.max(cash + marketable_asset + lending_money, equity_capital + account + borrowing_money);
        asset_sum = a;

        gamma = lending_money / asset_sum;

        cash = equity_capital + account + borrowing_money - (marketable_asset + lending_money);
    }

    public static double CalculateSurplass(ArrayList<Bank> banks){
        double sum_borrowing_surplus = 0.0;
        for (int i = 0; i < Constants.N; i++) {
            sum_borrowing_surplus += Math.max(banks.get(i).bs.borrowing_money - banks.get(i).bs.lending_money, 0.0);
        }
        return sum_borrowing_surplus;
    }

    public void Clear(){
        asset_sum = 0.0;
        marketable_asset = 0.0;
        borrowing_money = 0.0;
        equity_capital = 0.0;
        account = 0.0;
        lending_money = 0.0;
        gamma = 0.0;
        cash = 0.0;
    }

    public void OutputBalanceSheet(MarketAsset market){
        double var = Bank.calculate_VaR(market);
            System.out.println("総資産　　：" + asset_sum);
            System.out.println("市場性資産：" + marketable_asset);
            System.out.println("貸出     ：" + lending_money);
            System.out.println("現金　　　：" + cash);
            System.out.println("自己資本　：" + equity_capital);
            System.out.println("預金　　　：" + account);
            System.out.println("借入　　　：" + borrowing_money);
            System.out.println("持株数　　：" + num_stocks);
            System.out.println("VaRf　　 ：" + EquityCapitalRatio(var)  );
            System.out.println();
    }

    public void Initialize(int outDegree, ArrayList<Bank> banks, double sum_marketable_assets, MarketAsset market, Random rand) {
        double sum_borrowing_surplus = CalculateSurplass(banks);
        MakeBalanceSheet(outDegree, sum_borrowing_surplus, sum_marketable_assets, market, rand);
    }
}

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
    double EquityCapitalRatio(ArrayList<Double> varlist) {
        double sum = 0.0;
        for (int i = 0; i < Constants.VaR.M; i++) {
            sum += (Math.abs(num_stocks[i]) * varlist.get(i));
        }
        return Constants.VaR.Control * equity_capital / sum;
    }
    int[] num_stocks = new int[Constants.VaR.M]; // 持ち株数
    double cash;             // 現金


    public static ArrayList<Map<Integer, Double>> MakeOmega(ArrayList<Bank> banks, double sum_marketable_assets, Random rand){
        ArrayList<Map<Integer, Double>> omega = new ArrayList<>();
        for(int i = 0; i < Constants.N; i++){
            omega.add(new HashMap<Integer, Double>());
        }
        double r = rand.nextDouble();
        double sum_lending_money = (Constants.BalanceSheet.gamma_whole / (1.0 - Constants.BalanceSheet.gamma_whole)) * sum_marketable_assets;

        int Omega_denominator = 0;
        for(int i = 0; i < Constants.N; i++){
            for(int j = 0; j < banks.get(i).neighborOut.size(); j++){
                Omega_denominator += Math.pow(banks.get(i).neighborOut.size() * banks.get(banks.get(i).neighborOut.get(j)).neighborOut.size(), r);
            }
        }
        for(int i = 0; i < Constants.N; i++){
            for(int j = 0; j < banks.get(i).neighborOut.size(); j++){
                double omega_i = Math.pow(banks.get(i).neighborOut.size() * banks.get(banks.get(i).neighborOut.get(j)).neighborOut.size(), r) * sum_lending_money / Omega_denominator;
                omega.get(i).put(banks.get(i).neighborOut.get(j), omega_i);
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

    public void MakeBalanceSheet(int outDegree, Double sum_borrowing_surplus, Double sum_marketable_assets, ArrayList<MarketAsset> markets, Random rand){
        double sum_lending_money = (Constants.BalanceSheet.gamma_whole / (1.0 - Constants.BalanceSheet.gamma_whole)) * sum_marketable_assets;
        ArrayList<Double> price_market = markets.get(0).getMarketPrice();

        double e = 0.0;
            double number_stock = 0.0;
        int number_stockInt = 0;

        e = Math.max(borrowing_money - lending_money, 0.0)
                + (sum_marketable_assets - sum_borrowing_surplus)
                * (lending_money / sum_lending_money);
        number_stock = e * Constants.VaR.stockmulti / price_market.get(price_market.size() - 1);
        for (int j = 0; j < number_stock; j++) {
            number_stockInt++;
        }
        e = 0.0;
        e = price_market.get(price_market.size() - 1) * number_stockInt / Constants.VaR.stockmulti;

        marketable_asset = e;
        num_stocks[0] = number_stockInt;

        account = outDegree * (30 + 10 * rand.nextDouble());

        double car = 0.6 + 0.1 * rand.nextDouble();

        double sum = 0.0;
        for (int i = 0; i < Constants.VaR.M; i++) {
            sum += (Math.abs(num_stocks[i]) * Bank.calculate_VaR(markets).get(i));
        }
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


        //System.out.println("sum_lending:" + sum_lending);
        //System.out.println("sum_borrowing" + sum_borrowing);


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
    public static void OutputBalanceSheet(ArrayList<Bank> banks, ArrayList<MarketAsset> markets){
        ArrayList<Double> varlist = Bank.calculate_VaR(markets);
        for(int i = 0; i < Constants.N; i++){
            System.out.println(i + "の総資産　　：" + banks.get(i).bs.asset_sum);
            System.out.println(i + "の市場性資産：" + banks.get(i).bs.marketable_asset);
            System.out.println(i + "の貸出     ：" + banks.get(i).bs.lending_money);
            System.out.println(i + "の現金　　　：" + banks.get(i).bs.cash);
            System.out.println(i + "の自己資本　：" + banks.get(i).bs.equity_capital);
            System.out.println(i + "の預金　　　：" + banks.get(i).bs.account);
            System.out.println(i + "の借入　　　：" + banks.get(i).bs.borrowing_money);
            System.out.println(i + "の持株数　　：" + banks.get(i).bs.num_stocks[0]);
            System.out.println(i + "のVaRf　　 ：" + banks.get(i).bs.EquityCapitalRatio(varlist)  );
            System.out.println();
        }
    }

    public void Initialize(int outDegree, ArrayList<Bank> banks, double sum_marketable_assets, ArrayList<MarketAsset> markets, Random rand) {
        double sum_borrowing_surplus = CalculateSurplass(banks);
        MakeBalanceSheet(outDegree, sum_borrowing_surplus, sum_marketable_assets, markets, rand);
    }
}

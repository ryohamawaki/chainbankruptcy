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

    public void MakeBalanceSheet(ArrayList<Bank> banks, Double sum_marketable_assets, ArrayList<MarketAsset> markets, Random rand){
        double sum_lending_money = (Constants.BalanceSheet.gamma_whole / (1.0 - Constants.BalanceSheet.gamma_whole)) * sum_marketable_assets;
        ArrayList<Double> price_market = markets.get(0).getMarketPrice();

        double[] borrowing_money_count = new double[(int)Constants.N];
        for(int i = 0; i < Constants. N; i++){
            for(int j = 0; j < banks.get(i).neighborOut.size(); j++){
                int k = banks.get(i).neighborOut.get(j);
                borrowing_money_count[k] += MakeOmega(banks, sum_marketable_assets, rand).get(i).get(k);
            }
        }

        for(int i = 0; i < Constants. N; i++) {
            banks.get(i).bs = new BalanceSheet();
            banks.get(i).bs.borrowing_money = borrowing_money_count[i];

            double lending_money_count = 0.0;
            for (int j = 0; j < banks.get(i).neighborOut.size(); j++) {
                lending_money_count += MakeOmega(banks, sum_marketable_assets, rand).get(i).get(banks.get(i).neighborOut.get(j));
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
            e = Math.max(banks.get(i).bs.borrowing_money - banks.get(i).bs.lending_money, 0.0)
                    + (sum_marketable_assets - sum_borrowing_surplus)
                    * (banks.get(i).bs.lending_money / sum_lending_money);
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

    public static void MakeBorrowingAndLendingList(ArrayList<Bank> banks, double sum_marketable_assets, Random rand){
        double sum_lending = 0.0;
        double sum_borrowing = 0.0;
        for(int i = 0; i < Constants.N; i++){
            for(int j = 0; j < banks.get(i).neighborOut.size(); j++){
                int k = banks.get(i).neighborOut.get(j);
                double get = MakeOmega(banks, sum_marketable_assets, rand).get(i).get(k);
                banks.get(i).List_lending.put(k, get);
                banks.get(k).List_borrowing.put(i, get);

                sum_lending += banks.get(i).List_lending.get(k);
                sum_borrowing += banks.get(k).List_borrowing.get(i);
            }
        }
        System.out.println("sum_lending:" + sum_lending);
        System.out.println("sum_borrowing" + sum_borrowing);
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

    public void Initialize(ArrayList<Bank> banks, double sum_marketable_assets, ArrayList<MarketAsset> markets, Random rand) {
        MakeBalanceSheet(banks, sum_marketable_assets, markets, rand);
        MakeBorrowingAndLendingList(banks, sum_marketable_assets, rand);
    }
}

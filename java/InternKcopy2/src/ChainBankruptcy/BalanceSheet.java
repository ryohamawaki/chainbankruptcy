package ChainBankruptcy;

import java.util.ArrayList;

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
}

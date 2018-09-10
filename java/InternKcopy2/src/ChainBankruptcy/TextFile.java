package ChainBankruptcy;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class TextFile {

    public static void OutputBalanceSheet(Bank bank, double var, int t) {

        try {
            //出力先を作成する
            File           file = new File("balancesheet_sh.txt");
            FileWriter     fw   = new FileWriter(file, true);  //※１：2番目の引数をtrueにすると追記モード、falseにすると上書きモードになります。
            BufferedWriter bw   = new BufferedWriter(fw);
            PrintWriter    pw   = new PrintWriter(bw);

            bw.write(t                               + " ");
            bw.write(bank.bs.asset_sum               + " ");
            bw.write(bank.bs.marketable_asset        + " ");
            bw.write(bank.bs.lending_money           + " ");
            bw.write(bank.bs.cash                    + " ");
            bw.write(bank.bs.equity_capital          + " ");
            bw.write(bank.bs.account                 + " ");
            bw.write(bank.bs.borrowing_money         + " ");
            bw.write(bank.bs.num_stocks              + " ");
            bw.write(bank.bs.EquityCapitalRatio(var) + " ");
            bw.newLine();

            //ファイルに書き出す
            pw.close();

            //終了メッセージを画面に出力する
            System.out.println("出力が完了しました。");

        } catch (IOException ex) {
            //例外時処理
            ex.printStackTrace();
        }
    }

    public static void OutputStatics(int num_bankrupt) {

        try {
            //出力先を作成する
            File           file = new File("_output.json");
            FileWriter     fw   = new FileWriter(file, true);  //※１：2番目の引数をtrueにすると追記モード、falseにすると上書きモードになります。
            BufferedWriter bw   = new BufferedWriter(fw);
            PrintWriter    pw   = new PrintWriter(bw);

            bw.write("{\"num_bankrupt\":" );
            bw.write(Integer.toString(num_bankrupt));
            bw.write("}");
            bw.newLine();

            //ファイルに書き出す
            pw.close();

            //終了メッセージを画面に出力する
            System.out.println("出力が完了しました。");

        } catch (IOException ex) {
            //例外時処理
            ex.printStackTrace();
        }
    }

}



package ChainBankruptcy;

import java.util.ArrayList;

public class Constants {
    public static final double N         = 100;
    public static final int    NInt      = 100;
    public static final double LargeN    = 10;                                 //大銀行の数
    public static final int    LargeNInt = 10;
    public static final double SmallN    = N - LargeN;

    public static class VaR {
        public static final int    m                = 100;                     //VaRの計算に利用
        public static final int    tp               = 60;                      //VaRの計算に利用・過去何日分のデータを取ってくるか
        public static final double r_f              = 0.02;                    //リスクフリーレート
        public static final double delta_t          = 0.004;                   //Δt
        public static final double sigma            = 0.25;                    //ボラティリティ
        public static final double Threshold        = 0.04;                    //VaR制約の閾値
        public static final int    stockmulti       = 20;                      //株価を何倍に換算するか　＝　外部資産の与える影響の制御
        public static final double Control          = 0.08 * stockmulti / 20;  //VaR制約の任意の係数
        public static final int    threshold_under5 = tp * 5 / 100;
    }

    public static final int rupttime = 1;

    public static class Args{
        public static int    trial_num;
        public static int    start_index;
        public static double coefficient_price_fluctuation;
        public static int    kind_of_network;
        public static int    num_of_link_scalefree;
        public static int    num_of_largelink;
        public static int    num_of_smalllink;
        public static double under_car;
        public static double width;
        public static int    time;

        public static void substituteArgs(String[] args){
            trial_num                     = Integer.parseInt(args[0]);
            start_index                   = Integer.parseInt(args[1]);
            coefficient_price_fluctuation = Integer.parseInt(args[2]);
            kind_of_network               = Integer.parseInt(args[3]);
            num_of_link_scalefree         = Integer.parseInt(args[4]);
            num_of_largelink              = Integer.parseInt(args[5]);
            num_of_smalllink              = Integer.parseInt(args[6]);
            under_car                     = Integer.parseInt(args[7]);
            width                         = Integer.parseInt(args[8]);
            time                          = Integer.parseInt(args[9]);
        }
    }

    public static class Network{
        public static final int    number_smallgroup             = (int) (SmallN / LargeN);
        public static final double p_large_coreperiphery         = (Args.num_of_largelink - 18.0) / 90.0;

        public static final double p_small_cluster_coreperiphery = (44.0 * Args.num_of_smalllink - 251.0 - 440.0 * p_large_coreperiphery) / 7832.0;
        public static final double p_small_coreperiphery         = (11.0 * Args.num_of_smalllink + 4.0 - 110.0 * p_large_coreperiphery) / 1958.0;

        public static final double p_small_random                = (18.0 * Args.num_of_smalllink - 5.0 * Args.num_of_largelink / 3.0) / 3234.0;
        public static final double p_large_random                = (Args.num_of_smalllink / 10.0) - (18.8 * p_small_random);
    }

    public static class BalanceSheet{
        public static final double gamma_whole = 0.50;
    }

    public static class FCN{
        public static final double tauF       = 0.01;
        public static final int    tauC       = 50;
        public static final double noiseScale = 0.001;
    }
}

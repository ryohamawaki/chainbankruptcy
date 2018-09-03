package ChainBankruptcy;

import java.util.ArrayList;

public class Constants {
    public static final double N = 100;
    public static final int NInt = 100;
    public static final double LargeN = 10;                                        //大銀行の数
    public static final int LargeNInt = 10;
    public static final double SmallN = N - LargeN;

    public static class VaR {
        public static final int m = 100;                    //VaRの計算に利用
        public static final int tp = 60;                    //VaRの計算に利用・過去何日分のデータを取ってくるか
        public static final double r_f = 0.02;                    //リスクフリーレート
        public static final double delta_t = 0.004;                //Δt
        public static final double sigma = 0.25;                    //ボラティリティ
        public static final int M = 1;                        //外部資産数
        public static final double Threshold = 0.04;                    //VaR制約の閾値
        public static final double ThresholdF = 0.08;                    //資金繰りの時のVaR制約の閾値
        public static final int stockmulti = 20;                    //株価を何倍に換算するか　＝　外部資産の与える影響の制御
        public static final double Control = 0.2 * stockmulti / 20;            //VaR制約の任意の係数
        public static final int threshold_under5 = tp * 5 / 100;
    }

    public static final int divnum = 10;						//何分割にして他の銀行から資金繰りするか

    public static final int time = 10;
    public static final int rupttime = 1;

    public static class Args{
        public static final boolean output = false;
        public static final int trial_num = 1;
        public static final int start_index = 0;
        public static final double coefficient_price_fluctuation = 0.5;
        public static final double p_large_to_smal = 0.5;
        public static final int kind_of_network = 2;
        public static final int num_of_link_scalefree = 8;
        public static final int num_of_largelink = 20;
        public static final int num_of_smalllink = 8;
    }

    public static class Network{
        public static final int number_smallgroup = (int) (SmallN / LargeN);
        public static final double p_large_coreperiphery = (Args.num_of_largelink - 18.0) / 90.0;

        public static final double p_small_cluster_coreperiphery = (44.0 * Args.num_of_smalllink - 251.0 - 440.0 * p_large_coreperiphery) / 7832.0;
        public static final double p_small_coreperiphery = (11.0 * Args.num_of_smalllink + 4.0 - 110.0 * p_large_coreperiphery) / 1958.0;

        public static final double p_small_random = (18.0 * Args.num_of_smalllink - 5.0 * Args.num_of_largelink / 3.0) / 3234.0;
        public static final double p_large_random = (Args.num_of_smalllink / 10.0) - (18.8 * p_small_random);
    }

    public static class BalanceSheet{
        public static final double gamma_whole = 0.50;
    }

    public static class FCN{
        public static final double tauF = 0.01;
        public static final int tauC = 50;
        public static final double noiseScale = 0.001;
    }
}

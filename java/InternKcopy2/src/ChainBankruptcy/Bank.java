package ChainBankruptcy;

import java.util.*;

public class Bank {
    double index;
    double expected_return;
    double gap;

    boolean status;

    int sum_link_out;

    ArrayList<Integer> neighbor = new ArrayList<Integer>(); // [TODO] 削除。neighborOutとneighborInから作って返すメソッドを定義する
    ArrayList<Integer> neighborOut = new ArrayList<Integer>();
    ArrayList<Integer> neighborIn = new ArrayList<Integer>();

    Map<Integer, Double> Omega = new HashMap<>();

    ArrayList<Double> BalanceSheet = new ArrayList<Double>();  // [TODO] BalanceSheetクラスで置き換える
    ArrayList<Boolean> VaRjudge = new ArrayList<Boolean>();   // [TODO] メンバー変数の削除。メソッドを作る

    double count_borrowing_money; // [TODO] 削除。メソッドで置き換える。その都度List_borrowingなどから計算できるはず
    Map<Integer, Double> List_borrowing = new HashMap<>();
    Map<Integer, Double> List_lending = new HashMap<>();


    public Bank(int id, boolean bankstatus){
        index = id;
        status = bankstatus;
    }

    public static ArrayList<Bank> InitializeInterbankNetwork(Random rand){
        ArrayList<Bank> banks = new ArrayList<>();
        for(int i = 0; i < Constants.N; i++){
            banks.add(new Bank(i, true)) ;
        }
        MakeNetwork(banks, Constants.Args.kind_of_network, rand);
        AssignLinkDirection(banks, rand);
        return banks;
    }

    public static void InitializeBalanceSheet(ArrayList<Bank> banks, double sum_marketable_assets, ArrayList<MarketAsset> markets, Random rand){
        MakeOmega(banks, sum_marketable_assets, rand);
        MakeBalanceSheet(banks, sum_marketable_assets, markets, rand);
        MakeBorrowingAndLendingList(banks);
    }

    public static void BuyOrSellMarketableAssets(ArrayList<Bank> banks, ArrayList<MarketAsset> markets, Random rand){
        judge_VaR(banks, markets);
        calculate_expected_return(banks, markets, rand);
        Buy_or_Sell(banks);
        MarketAsset.deal_marketable_assets(banks, markets, rand);
    }

    public static void MakeNetwork(ArrayList<Bank> banks, int kind_of_network, Random rand){
        if(kind_of_network == 1){
            for(int i = 0; i < Constants.LargeNInt; i++){
                for(int j = 0; j < i; j++){
                    banks.get(i).neighbor.add(j);
                    banks.get(j).neighbor.add(i);
                }

            }
            for(int i = Constants.LargeNInt; i < Constants.N; i++){
                int number_hub = (i - Constants.LargeNInt) / Constants.Network.number_smallgroup;
                banks.get(i).neighbor.add(number_hub);
                banks.get(number_hub).neighbor.add(i);

                int index_each_smallgroup = (i-1) % Constants.Network.number_smallgroup;
                if(index_each_smallgroup == 8){
                    for(int j = i - 8; j < i-4; j++){
                        for(int k = i - 8; k < j; k++){
                            banks.get(j).neighbor.add(k);
                            banks.get(k).neighbor.add(j);
                        }
                    }
                }
            }
            boolean overlap_coreperiphery = false;

            for(int i = 0; i < Constants.LargeNInt; i++){
                for(int j = Constants.LargeNInt; j < Constants.N; j++){
                    for(int k = 0; k < banks.get(i).neighbor.size(); k++){
                        if(banks.get(i).neighbor.get(k) == j){
                            overlap_coreperiphery = true;
                        }
                    }

                    if(overlap_coreperiphery){
                        overlap_coreperiphery = false;
                        continue;
                    }else{
                        if(rand.nextDouble() < Constants.Network.p_large_coreperiphery){
                            banks.get(i).neighbor.add(j);
                            banks.get(j).neighbor.add(i);
                        }
                    }
                }
            }

            for(int i = Constants.LargeNInt; i < Constants.N; i++){
                for(int j = Constants.LargeNInt; j < Constants.N; j++){
                    if(i == j){
                        continue;
                    }
                    for(int k = 0; k < banks.get(i).neighbor.size(); k++) {
                        if (banks.get(i).neighbor.get(k) == j) {
                            overlap_coreperiphery = true;
                        }
                    }
                    if(overlap_coreperiphery){
                        overlap_coreperiphery = false;
                        continue;
                    }else{
                        if((j - 1) % 9 < 4){
                            if(rand.nextDouble() < Constants.Network.p_small_cluster_coreperiphery){
                                banks.get(i).neighbor.add(j);
                                banks.get(j).neighbor.add(i);
                            }
                        }else{
                            if(rand.nextDouble() < Constants.Network.p_small_coreperiphery){
                                banks.get(i).neighbor.add(j);
                                banks.get(j).neighbor.add(i);
                            }
                        }
                    }
                }
            }
        }
        if(kind_of_network == 2){
            for(int i = 0; i < Constants.LargeNInt; i++){
                for(int j = 0; j < i; j++){
                    banks.get(i).neighbor.add(j);
                    banks.get(j).neighbor.add(i);
                }

            }
            for(int i = Constants.LargeNInt; i < Constants.N; i++){
                roulette_selection(banks, i, Constants.Args.num_of_link_scalefree, rand);
            }
        }
        if(kind_of_network == 3){
            for(int i = 0; i < Constants.LargeNInt; i++){
                for(int j = 0; j < Constants.NInt; j++){
                    if(rand.nextDouble() <= Constants.Network.p_large_random) {
                        banks.get(i).neighbor.add(j);
                        banks.get(j).neighbor.add(i);
                    }
                }
            }
            for(int i = Constants.LargeNInt; i < Constants.NInt; i++){
                for(int j = 0; j < Constants.NInt; j++){
                    if(rand.nextDouble() <= Constants.Network.p_small_random) {
                        banks.get(i).neighbor.add(j);
                        banks.get(j).neighbor.add(i);
                    }
                }
            }
        }
    }

    public static void AssignLinkDirection(ArrayList<Bank> banks, Random rand){
        ArrayList<ArrayList<Integer>> preneighbors = new ArrayList<ArrayList<Integer>>(banks.size());
        for(int i = 0; i < Constants.N; i++){
            ArrayList<Integer> a = new ArrayList<>();
            preneighbors.add(a);
            for(int j = 0; j < banks.get(i).neighbor.size(); j++) {
                preneighbors.get(i).add(banks.get(i).neighbor.get(j));
            }
        }

        for(int i = 0; i < Constants.N; i++){
            for(int j = 0; j < preneighbors.get(i).size(); j++){
                if((i < Constants.LargeN && banks.get(i).neighbor.get(j) < Constants.LargeN) || (i >= Constants.LargeN && banks.get(i).neighbor.get(j) >= Constants.LargeN)){
                    int k = preneighbors.get(i).get(j);
                    if(rand.nextDouble() <= 0.5){
                        banks.get(i).neighborOut.add(k);
                    }else{
                        banks.get(k).neighborOut.add(i);
                    }
                    preneighbors.get(k).remove(preneighbors.get(k).indexOf(i));

                    //変更したい
                    if(banks.get(i).neighborOut.size() == 0){
                        banks.get(k).neighborOut.remove(banks.get(k).neighborOut.indexOf(i));
                        preneighbors.get(k).add(i);
                        j--;
                        continue;
                    }
                }

                if(i < Constants.LargeN && banks.get(i).neighbor.get(j) >= Constants.LargeN){
                    int k = preneighbors.get(i).get(j);
                    if(rand.nextDouble() <= 0.5){
                        banks.get(i).neighborOut.add(k);
                    }else{
                        banks.get(k).neighborOut.add(i);
                    }
                    preneighbors.get(k).remove(preneighbors.get(k).indexOf(i));

                    //変更したい
                    if(banks.get(i).neighborOut.size() == 0){
                        banks.get(k).neighborOut.remove(banks.get(k).neighborOut.indexOf(i));
                        preneighbors.get(k).add(i);
                        j--;
                        continue;
                    }
                }


            }
        }
        for(int i = 0; i < Constants.N; i++){
            for(int j = 0; j < banks.get(i).neighborOut.size(); j++){
                banks.get(banks.get(i).neighborOut.get(j)).neighborIn.add(i);
            }
        }
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


    public static void MakeBalanceSheet(ArrayList<Bank> banks, Double sum_marketable_assets, ArrayList<MarketAsset> markets, Random rand){
        double sum_lending_money = (Constants.BalanceSheet.gamma_whole / (1.0 - Constants.BalanceSheet.gamma_whole)) * sum_marketable_assets;
        ArrayList<Double> price_market = markets.get(0).getMarketPrice();

        for(int i = 0; i < Constants.N; i++){
            for(int j = 0; j < 10; j++){
                banks.get(i).BalanceSheet.add(0.0);
            }
        }

        for(int i = 0; i < Constants. N; i++){
            for(int j = 0; j < banks.get(i).neighborOut.size(); j++){
                int k = banks.get(i).neighborOut.get(j);
                banks.get(k).count_borrowing_money += banks.get(i).Omega.get(k);
            }
        }

        for(int i = 0; i < Constants. N; i++){
            banks.get(i).BalanceSheet.set(5, banks.get(i).count_borrowing_money);

            double l = 0.0;
            for(int j = 0; j < banks.get(i).neighborOut.size(); j++){
                l += banks.get(i).Omega.get(banks.get(i).neighborOut.get(j));
            }
            banks.get(i).BalanceSheet.set(2, l);

            double e = 0.0;
            double sum_borrowing_surplus = 0.0;
            double number_stock = 0.0;
            int number_stockInt = 0;

            for(int j = 0; j < Constants.N; j++){
                sum_borrowing_surplus += Math.max(banks.get(j).BalanceSheet.get(5) - banks.get(j).BalanceSheet.get(2), 0.0);
            }
            e = Math.max(banks.get(i).BalanceSheet.get(5) - banks.get(i).BalanceSheet.get(2), 0.0) + (sum_marketable_assets - sum_borrowing_surplus) * (banks.get(i).BalanceSheet.get(2) / sum_lending_money);
            number_stock = e * Constants.VaR.stockmulti / price_market.get(price_market.size() - 1);
            for(int j = 0; j < number_stock; j++){
                number_stockInt++;
            }
            e = 0.0;
            e = price_market.get(price_market.size() - 1) * number_stockInt / Constants.VaR.stockmulti;

            banks.get(i).BalanceSheet.set(1, e);
            banks.get(i).BalanceSheet.set(8, (double)number_stockInt);

            double d = banks.get(i).neighborOut.size() * (30 + 10 * rand.nextDouble());
            banks.get(i).BalanceSheet.set(4, d);

            double car = 0.1 + 0.2 * rand.nextDouble();
            double c = (banks.get(i).BalanceSheet.get(4) + banks.get(i).BalanceSheet.get(5)) * (car / (1 - car));
            banks.get(i).BalanceSheet.set(3, c);
            banks.get(i).BalanceSheet.set(7, car);

            double a = Math.max(banks.get(i).BalanceSheet.get(9) + banks.get(i).BalanceSheet.get(1) + banks.get(i).BalanceSheet.get(2), banks.get(i).BalanceSheet.get(3) + banks.get(i).BalanceSheet.get(4) + banks.get(i).BalanceSheet.get(5));
            banks.get(i).BalanceSheet.set(0, a);

            double gamma = banks.get(i).BalanceSheet.get(2) / banks.get(i).BalanceSheet.get(0);
            banks.get(i).BalanceSheet.set(6, gamma);

            double money = banks.get(i).BalanceSheet.get(3) + banks.get(i).BalanceSheet.get(4) + banks.get(i).BalanceSheet.get(5) - (banks.get(i).BalanceSheet.get(1) + banks.get(i).BalanceSheet.get(2));
            banks.get(i).BalanceSheet.set(9, money);
        }
    }

    public static void update_Gap(ArrayList<Bank> banks){
        for(int i = 0; i < Constants.N; i++){
            banks.get(i).gap = -(banks.get(i).BalanceSheet.get(9) + banks.get(i).BalanceSheet.get(1) + banks.get(i).BalanceSheet.get(2)
                    - banks.get(i).BalanceSheet.get(3) - banks.get(i).BalanceSheet.get(4) - banks.get(i).BalanceSheet.get(5));
        }
    }


    public static void roulette_selection(ArrayList<Bank> banks, int id, int num_of_link, Random rand){
        for(int i = 0; i < num_of_link; i++){
            int sum_link = 0;
            for(int j = 0; j < Constants.N; j++){
                sum_link += banks.get(j).neighbor.size();
            }
            double target_number = rand.nextDouble();
            int connected_id = -1;

            for(int j = 0; target_number > 0; j++){
                double minus_number = -((double)banks.get(j).neighbor.size() / sum_link);
                target_number += minus_number;
                connected_id++;
            }

            boolean overlap_connected_id = true;
            if(id == connected_id){
                overlap_connected_id = false;
            }
            for(int j = 0; j < banks.get(id).neighbor.size(); j++){
                if(banks.get(id).neighbor.get(j) == connected_id){
                    overlap_connected_id = false;
                }
            }
            if(overlap_connected_id){
                banks.get(id).neighbor.add(connected_id);
                banks.get(connected_id).neighbor.add(id);
            }
            if(!overlap_connected_id){
                i--;
            }
        }
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
    public static void calculate_expected_return(ArrayList<Bank> banks, ArrayList<MarketAsset> markets, Random rand){
        ArrayList<Double> fundamental_price = markets.get(0).getPrice();		//理論価格の取得
        ArrayList<Double> market_price = markets.get(0).getMarketPrice();	//市場価格の取得
        double fundamental_LogReturn = Constants.FCN.tauF * Math.log(fundamental_price.get(fundamental_price.size()-1) / market_price.get(market_price.size()-1));

        double chartMean_LogReturn = 0;
        for(int i = (market_price.size() - 1); i > (market_price.size() - Constants.FCN.tauC - 1); i--){
            chartMean_LogReturn += Math.log(market_price.get(i)/market_price.get(i-1));
        }
        chartMean_LogReturn = chartMean_LogReturn / Constants.FCN.tauC;

        double noise_LogReturn = 0.0 + Constants.FCN.noiseScale * rand.nextGaussian();

        for(int i = 0; i < Constants.NInt; i++){
            double weight_F = 5 * rand.nextDouble();
            double weight_C = rand.nextDouble();
            double weight_N = rand.nextDouble();
            double norm = weight_F + weight_C + weight_N;

            banks.get(i).expected_return = (weight_F * fundamental_LogReturn + weight_C * chartMean_LogReturn + weight_N * noise_LogReturn) / norm;
        }
    }

    public static void judge_VaR(ArrayList<Bank> banks, ArrayList<MarketAsset> markets){
        ArrayList<Double> VaRList = calculate_VaR(markets);

        for(int i = 0; i < Constants.N; i++){
            for(int j = 0; j < 10; j++){
                banks.get(i).VaRjudge.add(true);
            }
            ArrayList<Boolean> each_VaRjudge = new ArrayList<Boolean>();
            for(int j = 0; j < Constants.VaR.M; j++){
                boolean judge_VaR = false;
                double VaRf = Constants.VaR.Control * banks.get(i).BalanceSheet.get(3) / (Math.abs(banks.get(i).BalanceSheet.get(8)) * VaRList.get(j));
                banks.get(i).BalanceSheet.set(7, VaRf);

                if(VaRf >= Constants.VaR.Threshold){
                    judge_VaR = true;
                }
                each_VaRjudge.add(judge_VaR);
            }
            banks.get(i).VaRjudge.set(0, each_VaRjudge.get(0));
        }
    }
    public static ArrayList<Double> calculate_VaR(ArrayList<MarketAsset> markets){
        ArrayList<Double> VaRList = new ArrayList<Double>();
        ArrayList<Double> log_return_List = new ArrayList<Double>();

        for(int i = 0; i < Constants.VaR.M; i++){
            ArrayList<Double> price = markets.get(i).getMarketPrice();
            for(int j = price.size() - 1; j > price.size() - Constants.VaR.tp - 1; j--) {
                log_return_List.add(Math.log10(price.get(j) / price.get(j - 1)));
            }
            for(int j = 0; j < log_return_List.size(); j++){
                Collections.sort(log_return_List);
            }

            VaRList.add(Math.pow(10, log_return_List.get(Constants.VaR.threshold_under5)));
        }
        return VaRList;
    }

    //☆VaR制約→売り買いで「買い」＝＋１、「売り」＝−１を返す。
    public static ArrayList<Integer> Buy_or_Sell(ArrayList<Bank> banks){
        ArrayList<Integer> bors = new ArrayList<Integer>();			//結果を格納する配列
        ArrayList<Boolean> fcnjudge = judge_FCN(banks);
        for(int i = 0; i < Constants.N; i++){
            int Plus_or_Minus = 0;
            if(banks.get(i).status){
                if(Plus_or_Minus == 0){
                    if(!banks.get(i).VaRjudge.get(0)){
                        Plus_or_Minus = -1;
                    }else{
                        if(fcnjudge.get(i)){
                            Plus_or_Minus = 1;
                        }else{
                            Plus_or_Minus = -1;
                        }
                    }
                }

            }
            bors.add(Plus_or_Minus);
        }
        return(bors);
    }
    public static ArrayList<Boolean> judge_FCN(ArrayList<Bank> banks){
        ArrayList<Boolean> each_judge_fcn = new ArrayList<Boolean>();		//結果を格納する配列

        for(int i = 0; i < Constants.N; i++){
            boolean judge_fcn = false;
            if(banks.get(i).expected_return >= 0){
                judge_fcn = true;
            }
            each_judge_fcn.add(judge_fcn);
        }
        return each_judge_fcn;
    }

    public static void UpdateBalanceSheet(ArrayList<Bank> banks, ArrayList<MarketAsset> markets){
        ArrayList<Double> marketprice = markets.get(0).getMarketPrice();

        for(int i = 0; i < Constants.N; i++){
            if(!banks.get(i).status){
                continue;
            }
            double e_update = 0.0;
            e_update = banks.get(i).BalanceSheet.get(8) * marketprice.get(marketprice.size() - 1) / Constants.VaR.stockmulti;
            banks.get(i).BalanceSheet.set(1, e_update);	//外部資産はBS(8)：持ち株数 * Mp（最新時刻）：市場価格から算出
        }

        for(int i = 0; i < Constants.N; i++){
            double l_update = 0.0;
            for(int j = 0; j < banks.get(i).neighborOut.size(); j++){
                l_update += banks.get(i).List_lending.get(banks.get(i).neighborOut.get(j));	//貸出額はLendListの総和から算出
            }
            banks.get(i).BalanceSheet.set(2, l_update);
        }

        for(int i = 0; i < Constants.N; i++){
            double b_update = 0.0;
            for(int j = 0; j < banks.get(i).neighborIn.size(); j++){
                b_update += banks.get(i).List_borrowing.get(banks.get(i).neighborIn.get(j));	//借入額はBorrowListの総和から算出
            }
            banks.get(i).BalanceSheet.set(5, b_update);
        }

        for(int i = 0; i < Constants.N; i++){
            double c_update = 0.0;
            double gap = -(banks.get(i).BalanceSheet.get(9) + banks.get(i).BalanceSheet.get(1) + banks.get(i).BalanceSheet.get(2)
                    - banks.get(i).BalanceSheet.get(3) - banks.get(i).BalanceSheet.get(4) - banks.get(i).BalanceSheet.get(5));
            c_update = banks.get(i).BalanceSheet.get(3) - gap + 0.0001;   //0.0001は浮動小数点対策

            banks.get(i).BalanceSheet.set(3, c_update);
        }

        for(int i = 0; i < Constants.N; i++){
            double a_update = 0.0;
            a_update = Math.max(banks.get(i).BalanceSheet.get(9) + banks.get(i).BalanceSheet.get(1) + banks.get(i).BalanceSheet.get(2),
                    banks.get(i).BalanceSheet.get(3) + banks.get(i).BalanceSheet.get(4) + banks.get(i).BalanceSheet.get(5));	//資産a=max(外部資産e+銀行間貸出l, 自己資本c+預金d+銀行間借入b)
            banks.get(i).BalanceSheet.set(0, a_update);
        }
        update_Gap(banks);
    }

    public static void GoEachBankrupt(ArrayList<Bank> banks, ArrayList<MarketAsset> markets){
        judge_VaR(banks, markets);
        System.out.print("この回倒産したのは");

        for(int i = 0; i < Constants.N; i++){
            if(banks.get(i).status == false){
                continue;
            }
            //CAR<ThresholdまたはGap<0の時に銀行は倒産
            if(!banks.get(i).VaRjudge.get(0)){
                double VaRf = Constants.VaR.Control * banks.get(i).BalanceSheet.get(3) / (Math.abs(banks.get(i).BalanceSheet.get(8)) * 0.98);

                GoBankrupt(banks, i);
                System.out.print(" " + i +",");
            }
            //if(bankArray(i).Gap < 0){
            //bankrupt(bankArray, i);
            //}
        }
        System.out.println();
    }
    public static void GoBankrupt(ArrayList<Bank> banks, int ruptID){
        banks.get(ruptID).status = false;		//状態を１→０に変える

        //貸し借り表の値を全て０にする
        for(int i = 0; i < banks.get(ruptID).neighborIn.size(); i++){
            //BorrowListを０にする
            banks.get(ruptID).List_borrowing.remove(banks.get(ruptID).neighborIn.get(i));
            banks.get(ruptID).List_borrowing.put(banks.get(ruptID).neighborIn.get(i), 0.0);
            //倒産した銀行に貸出していた銀行のLendListを０にする
            banks.get(banks.get(ruptID).neighborIn.get(i)).List_lending.remove(ruptID);
            banks.get(banks.get(ruptID).neighborIn.get(i)).List_lending.put(ruptID, 0.0);
        }

        for(int i = 0; i < banks.get(ruptID).neighborOut.size(); i++){
            //LendListを０にする
            banks.get(ruptID).List_lending.remove(banks.get(ruptID).neighborOut.get(i));
            banks.get(ruptID).List_lending.put(banks.get(ruptID).neighborOut.get(i), 0.0);
            //倒産した銀行から借入れていた銀行のBorrowListを０にする
            banks.get(banks.get(ruptID).neighborOut.get(i)).List_borrowing.remove(ruptID);
            banks.get(banks.get(ruptID).neighborOut.get(i)).List_borrowing.put(ruptID, 0.0);
        }
        for(int i = 0; i < banks.get(ruptID).neighbor.size(); i++){
            //NeighborからruptIDを外す
            int n = banks.get(ruptID).neighbor.get(i);
            // System.err.println("" + i + " " + ruptID + " " + n);
            Bank bank_n = banks.get(n);
            bank_n.neighbor.remove(bank_n.neighbor.indexOf(ruptID));
        }



        //BSの値を全て０にする
        for(int i = 0; i < 10; i++){
            banks.get(ruptID).BalanceSheet.set(i, 0.0);		//外部資産はとりあえず売ったりしない
        }

    }

    public static int countrupt(ArrayList<Bank> banks){
        int ruptnum = 0;
        for(int i = 0; i < Constants.N; i++){
            if(!banks.get(i).status){
                ruptnum++;
            }
        }
        return ruptnum;

    }

}

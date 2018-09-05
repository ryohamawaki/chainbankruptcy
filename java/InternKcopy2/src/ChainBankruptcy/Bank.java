package ChainBankruptcy;

import java.lang.reflect.Array;
import java.util.*;

public class Bank {
    int index;

    boolean status;

    //int sum_link_out; // [TODO] これも削除 completed

    ArrayList<Integer> neighborOut = new ArrayList<Integer>();
    ArrayList<Integer> neighborIn = new ArrayList<Integer>();

    //Map<Integer, Double> Omega = new HashMap<>(); // [TODO] これも削除 completed

    BalanceSheet bs;

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
        return banks;
    }

    public static void BuyOrSellMarketableAssets(ArrayList<Bank> banks, ArrayList<MarketAsset> markets, Random rand){
        MarketAsset.deal_marketable_assets(banks, markets, rand);
    }

    public static void MakeNetwork(ArrayList<Bank> banks, int kind_of_network, Random rand){
        ArrayList<ArrayList<Integer>> neighbor = MakeUndirectedGraph(kind_of_network, rand);

        //ここから方向を決める
        ArrayList<ArrayList<Integer>> link_list = AssignDirection(neighbor, rand);

        // neighborOutにdirectedの情報を設定する
        // [TODO link_listからneighborOutとneighborInを作る
        // ....
        LinklistToNeighborOutAndIn(banks, link_list);

    }
    public static ArrayList<ArrayList<Integer>> MakeNeighbor(ArrayList<Bank> banks){
        ArrayList<ArrayList<Integer>> neighbor = new ArrayList<>();
        for(int i = 0; i < Constants.N; i++){
            for(int j = 0; j < banks.get(i).neighborOut.size(); j++){
                neighbor.get(i).add(banks.get(i).neighborOut.get(j));
            }
            for(int j = 0; j < banks.get(i).neighborIn.size(); j++){
                neighbor.get(i).add(banks.get(i).neighborIn.get(j));
            }
        }
        return neighbor;
    }

    public static ArrayList<ArrayList<Integer>> MakeUndirectedGraph(int kind_of_network, Random rand){
        ArrayList<ArrayList<Integer>> neighbor = new ArrayList<>();
        for(int i = 0; i < Constants.N; i++){
            neighbor.add(new ArrayList<>());
        }

        if(kind_of_network == 1){
            for(int i = 0; i < Constants.LargeNInt; i++){
                for(int j = 0; j < i; j++){

                    neighbor.get(i).add(j);
                    neighbor.get(j).add(i);
                }

            }
            for(int i = Constants.LargeNInt; i < Constants.N; i++){
                int number_hub = (i - Constants.LargeNInt) / Constants.Network.number_smallgroup;
                neighbor.get(i).add(number_hub);
                neighbor.get(number_hub).add(i);

                int index_each_smallgroup = (i-1) % Constants.Network.number_smallgroup;
                if(index_each_smallgroup == 8){
                    for(int j = i - 8; j < i-4; j++){
                        for(int k = i - 8; k < j; k++){
                            neighbor.get(j).add(k);
                            neighbor.get(k).add(j);
                        }
                    }
                }
            }
            boolean overlap_coreperiphery = false;

            for(int i = 0; i < Constants.LargeNInt; i++){
                for(int j = Constants.LargeNInt; j < Constants.N; j++){
                    for(int k = 0; k < neighbor.get(i).size(); k++){
                        if(neighbor.get(i).get(k) == j){
                            overlap_coreperiphery = true;
                        }
                    }

                    if(overlap_coreperiphery){
                        overlap_coreperiphery = false;
                        continue;
                    }else{
                        if(rand.nextDouble() < Constants.Network.p_large_coreperiphery){
                            neighbor.get(i).add(j);
                            neighbor.get(j).add(i);
                        }
                    }
                }
            }

            for(int i = Constants.LargeNInt; i < Constants.N; i++){
                for(int j = Constants.LargeNInt; j < Constants.N; j++){
                    if(i == j){
                        continue;
                    }
                    for(int k = 0; k < neighbor.get(i).size(); k++) {
                        if (neighbor.get(i).get(k) == j) {
                            overlap_coreperiphery = true;
                        }
                    }
                    if(overlap_coreperiphery){
                        overlap_coreperiphery = false;
                        continue;
                    }else{
                        if((j - 1) % 9 < 4){
                            if(rand.nextDouble() < Constants.Network.p_small_cluster_coreperiphery){
                                neighbor.get(i).add(j);
                                neighbor.get(j).add(i);
                            }
                        }else{
                            if(rand.nextDouble() < Constants.Network.p_small_coreperiphery){
                                neighbor.get(i).add(j);
                                neighbor.get(j).add(i);
                            }
                        }
                    }
                }
            }
        }
        if(kind_of_network == 2){
            for(int i = 0; i < Constants.LargeNInt; i++){
                for(int j = 0; j < i; j++){
                    neighbor.get(i).add(j);
                    neighbor.get(j).add(i);
                }

            }
            for(int i = Constants.LargeNInt; i < Constants.N; i++){
                for(int j = 0; j < Constants.Args.num_of_link_scalefree; j++){
                    int sum_link = 0;
                    for(int k = 0; k < Constants.N; k++){
                        sum_link += neighbor.get(k).size();
                    }
                    double target_number = rand.nextDouble();
                    int connected_id = -1;

                    for(int k = 0; target_number > 0; k++){
                        double minus_number = -((double)neighbor.get(k).size() / sum_link);
                        target_number += minus_number;
                        connected_id++;
                    }

                    boolean overlap_connected_id = true;
                    if(i == connected_id){
                        overlap_connected_id = false;
                    }
                    for(int k = 0; k < neighbor.get(i).size(); k++){
                        if(neighbor.get(i).get(k) == connected_id){
                            overlap_connected_id = false;
                        }
                    }
                    if(overlap_connected_id){
                        neighbor.get(i).add(connected_id);
                        neighbor.get(connected_id).add(i);
                    }
                    if(!overlap_connected_id){
                        j--;
                    }
                }
            }
        }
        if(kind_of_network == 3){
            for(int i = 0; i < Constants.LargeNInt; i++){
                for(int j = 0; j < Constants.NInt; j++){
                    if(rand.nextDouble() <= Constants.Network.p_large_random) {
                        neighbor.get(i).add(j);
                        neighbor.get(j).add(i);
                    }
                }
            }
            for(int i = Constants.LargeNInt; i < Constants.NInt; i++){
                for(int j = 0; j < Constants.NInt; j++){
                    if(rand.nextDouble() <= Constants.Network.p_small_random) {
                        neighbor.get(i).add(j);
                        neighbor.get(j).add(i);
                    }
                }
            }
        }
        return neighbor;
    }

    public static ArrayList<ArrayList<Integer>> AssignDirection(ArrayList<ArrayList<Integer>> neighbor, Random rand){
        ArrayList<ArrayList<Integer>> link_list = new ArrayList<>();
        ArrayList<Integer> link_list_out = new ArrayList<>();
        ArrayList<Integer> link_list_in = new ArrayList<>();

        for(int i = 0; i < Constants.N; i++){
            for(int j = 0; j < neighbor.get(i).size(); j++){
                if((i < Constants.LargeN && neighbor.get(i).get(j) < Constants.LargeN) || (i >= Constants.LargeN && neighbor.get(i).get(j) >= Constants.LargeN)){
                    int k = neighbor.get(i).get(j);
                    if(rand.nextDouble() <= 0.5){
                        link_list_out.add(i);
                        link_list_in.add(k);
                        //banks.get(i).neighborOut.add(k);
                    }else{
                        link_list_out.add(k);
                        link_list_in.add(i);
                    }
                    neighbor.get(k).remove(neighbor.get(k).indexOf(i));

                    //変更したい
                    /*
                    if(banks.get(i).neighborOut.size() == 0){
                        banks.get(k).neighborOut.remove(banks.get(k).neighborOut.indexOf(i));
                        neighbor.get(k).add(i);
                        j--;
                        continue;
                    }
                    */
                }

                if(i < Constants.LargeN && neighbor.get(i).get(j) >= Constants.LargeN){
                    int k = neighbor.get(i).get(j);
                    if(rand.nextDouble() <= 0.5){
                        link_list_out.add(i);
                        link_list_in.add(k);
                    }else{
                        link_list_out.add(k);
                        link_list_in.add(i);
                    }
                    neighbor.get(k).remove(neighbor.get(k).indexOf(i));

                    //変更したい
                    /*
                    if(banks.get(i).neighborOut.size() == 0){
                        banks.get(k).neighborOut.remove(banks.get(k).neighborOut.indexOf(i));
                        neighbor.get(k).add(i);
                        j--;
                        continue;
                    }
                    */
                }


            }
        }
        link_list.add(link_list_out);
        link_list.add(link_list_in);
        return link_list;
    }

    public static void LinklistToNeighborOutAndIn(ArrayList<Bank> banks, ArrayList<ArrayList<Integer>> link_list){
        for(int i = 0; i < Constants.N ; i++){
            for(int j = 0; j < link_list.size(); j++){
                banks.get(i).neighborOut.add(link_list.get(0).get(j));
                banks.get(i).neighborIn.add(link_list.get(1).get(j));
            }
        }
    }

    public static double[] CalculateExpectedReturn(ArrayList<MarketAsset> markets, Random rand) {
        // [TODO] 実装 & calculate_expected... の削除
        double[] expected_return = new double[(int) Constants.N];
        ArrayList<Double> fundamental_price = markets.get(0).getPrice();        //理論価格の取得
        ArrayList<Double> market_price = markets.get(0).getMarketPrice();    //市場価格の取得
        double fundamental_LogReturn = Constants.FCN.tauF * Math.log(fundamental_price.get(fundamental_price.size() - 1) / market_price.get(market_price.size() - 1));

        double chartMean_LogReturn = 0;
        for (int i = (market_price.size() - 1); i > (market_price.size() - Constants.FCN.tauC - 1); i--) {
            chartMean_LogReturn += Math.log(market_price.get(i) / market_price.get(i - 1));
        }
        chartMean_LogReturn = chartMean_LogReturn / Constants.FCN.tauC;

        double noise_LogReturn = 0.0 + Constants.FCN.noiseScale * rand.nextGaussian();

        for (int i = 0; i < Constants.NInt; i++) {
            double weight_F = 5 * rand.nextDouble();
            double weight_C = rand.nextDouble();
            double weight_N = rand.nextDouble();
            double norm = weight_F + weight_C + weight_N;

            expected_return[i] = (weight_F * fundamental_LogReturn + weight_C * chartMean_LogReturn + weight_N * noise_LogReturn) / norm;
        }
        return expected_return;
    }

    public boolean judgeVaR(ArrayList<MarketAsset> markets) {
        // [TODO] ... remove judge_VaR completed
        Boolean varjudge = false;
        ArrayList<Double> varlist = calculate_VaR(markets);
        double VaRf = this.bs.EquityCapitalRatio(varlist);

        if(VaRf >= Constants.VaR.Threshold){
            varjudge = true;
        }
        return varjudge;
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
    public int BuyOrSell(ArrayList<MarketAsset> markets, Random rand){
        int plus_or_minus = 0;
            if(this.status){
                    if(!this.judgeVaR(markets)){
                        plus_or_minus = -1;
                    }else{
                        if(judge_FCN(markets, rand).get(this.index)){
                            plus_or_minus = 1;
                        }else{
                            plus_or_minus = -1;
                        }
                    }
            }
        return plus_or_minus;
    }
    public static ArrayList<Boolean> judge_FCN(ArrayList<MarketAsset> markets, Random rand){
        ArrayList<Boolean> each_judge_fcn = new ArrayList<Boolean>();		//結果を格納する配列

        for(int i = 0; i < Constants.N; i++){
            boolean judge_fcn = false;
            double [] e_returns = CalculateExpectedReturn(markets, rand);
            if(e_returns[i] >= 0){
                judge_fcn = true;
            }
            each_judge_fcn.add(judge_fcn);
        }
        return each_judge_fcn;
    }

    public static void GoEachBankrupt(ArrayList<Bank> banks, ArrayList<MarketAsset> markets){
        ArrayList<Double> varlist = calculate_VaR(markets);
        //System.out.print("倒産したのは: ");

        for(int i = 0; i < Constants.N; i++){
            if(banks.get(i).status == false){
                continue;
            }
            //CAR<ThresholdまたはGap<0の時に銀行は倒産
            if(!banks.get(i).judgeVaR(markets)){
                double VaRf = banks.get(i).bs.EquityCapitalRatio(varlist);
                //System.out.print(i + ", ");
                GoBankrupt(banks, i);
            }
            //if(bankArray(i).Gap < 0){
            //bankrupt(bankArray, i);
            //}
        }
        //System.out.println();
    }
    public static void GoBankrupt(ArrayList<Bank> banks, int ruptID){
        //ArrayList<ArrayList<Integer>> neighbor = MakeNeighbor(banks);
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
        for(int i = 0; i < banks.get(ruptID).neighborOut.size(); i++){
            //NeighborからruptIDを外す→neighborOutもしくはneighborInから消去
            /*int n = neighbor.get(ruptID).get(i);
            // System.err.println("" + i + " " + ruptID + " " + n);
            Bank bank_n = banks.get(n);
            bank_n.neighbor.remove(bank_n.neighbor.indexOf(ruptID));
            */
            int n_out = banks.get(ruptID).neighborOut.get(i);
            Bank bank_n = banks.get(n_out);
            bank_n.neighborIn.remove(bank_n.neighborIn.indexOf(ruptID));
        }
        for(int i = 0; i < banks.get(ruptID).neighborIn.size(); i++){
            int n_in = banks.get(ruptID).neighborIn.get(i);
            Bank bank_n = banks.get(n_in);
            bank_n.neighborOut.remove(bank_n.neighborOut.indexOf(ruptID));
        }
        //BSの値を全て０にする
        //BalanceSheet.OutputBalanceSheet(banks);
        BalanceSheet.isClear(banks, ruptID);
    }
    public void GoBankrupt() {
        // [TODO] .... 全体をいじるので難しい

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

    public static void OutputNetwork(ArrayList<Bank> banks){
        for (int i = 0; i < Constants.N; i++){
            for (int j = 0; j < banks.get(i).neighborOut.size(); j++){
                System.out.println(i + " " + banks.get(i).neighborOut.get(j));
            }
        }
    }

    public void InitializeBalanceSheet(ArrayList<Bank> banks, double sum_marketable_assets, ArrayList<MarketAsset> markets, Random rand) {
        this.bs.Initialize(banks, sum_marketable_assets, markets, rand);
        // [TODO]....completed
    }

    public void UpdateBalanceSheet(ArrayList<MarketAsset> markets){
        ArrayList<Double> marketprice = markets.get(0).getMarketPrice();
        if(status){
            double e_update = 0.0;
            e_update = CountUpNumStocks() * marketprice.get(marketprice.size() - 1) / Constants.VaR.stockmulti;
            bs.marketable_asset = e_update;	//外部資産はBS(8)：持ち株数 * Mp（最新時刻）：市場価格から算出

            double l_update = 0.0;
            for(int i = 0; i < neighborOut.size(); i++){
                l_update += List_lending.get(neighborOut.get(i));	//貸出額はLendListの総和から算出
            }
            bs.lending_money = l_update;

            double b_update = 0.0;
            for(int i = 0; i < neighborIn.size(); i++){
                b_update += List_borrowing.get(neighborIn.get(i));	//借入額はBorrowListの総和から算出
            }
            bs.borrowing_money = b_update;

            double c_update = 0.0;
            double gap = -(bs.cash + bs.marketable_asset + bs.lending_money
                    - bs.equity_capital - bs.account - bs.borrowing_money);
            c_update = bs.equity_capital - gap + 0.0001;   //0.0001は浮動小数点対策

            bs.equity_capital = c_update;

            double a_update = 0.0;
            a_update = Math.max(bs.cash + bs.marketable_asset + bs.lending_money,
                    bs.equity_capital + bs.account + bs.borrowing_money);	//資産a=max(外部資産e+銀行間貸出l, 自己資本c+預金d+銀行間借入b)
            bs.asset_sum = a_update;
        }
    }

    public int CountUpNumStocks(){
        int sum = 0;
        for(int i = 0; i < Constants.VaR.M; i++){
            sum += bs.num_stocks[i];
        }
        return sum;
    }

    public double CalculateGap() {
        return -(bs.cash + bs.marketable_asset + bs.lending_money
                - bs.equity_capital - bs.account - bs.borrowing_money);
    }
}

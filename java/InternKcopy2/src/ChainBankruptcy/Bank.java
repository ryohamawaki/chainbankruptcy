package ChainBankruptcy;

import java.util.*;

public class Bank {
    int index;
    boolean status;
    BalanceSheet bs;

    Map<Integer, Double> List_borrowing = new HashMap<>();
    Map<Integer, Double> List_lending = new HashMap<>();


    public Bank(int id, boolean bankstatus){
        index = id;
        status = bankstatus;
        bs = new BalanceSheet();
    }

    public static ArrayList<Bank> InitializeInterbankNetwork(Random rand, double sum_marketable_assets){
        ArrayList<Bank> banks = new ArrayList<>();
        for(int i = 0; i < Constants.N; i++){
            banks.add(new Bank(i, true)) ;
        }
        ArrayList<ArrayList<Integer>> directed_link_list = MakeNetwork(banks, Constants.Args.kind_of_network, rand);
        ArrayList<Map<Integer, Double>> weighted_dll = BalanceSheet.MakeOmega(directed_link_list, sum_marketable_assets, rand);
        BalanceSheet.MakeBorrowingAndLendingList(banks, weighted_dll);
        return banks;
    }

    private static ArrayList<ArrayList<Integer>> MakeNetwork(ArrayList<Bank> banks, int kind_of_network, Random rand){
        ArrayList<ArrayList<Integer>> neighbor = MakeUndirectedGraph(kind_of_network, rand);
        ArrayList<ArrayList<Integer>> link_list = AssignDirection(neighbor, rand);
        return link_list;
    }

    private static ArrayList<ArrayList<Integer>> MakeUndirectedGraph(int kind_of_network, Random rand){
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
        ArrayList<ArrayList<Integer>> links = new ArrayList<>();
        for(int i=0; i<Constants.N; i++) {
            links.add( new ArrayList<Integer>());
        }
        for(int i=0; i<Constants.N; i++) { // randomly choose out-going link for each node
            ArrayList<Integer> links_i = neighbor.get(i);
            if( links_i.size() == 0 ) { throw new RuntimeException("invalid network"); }
            int r = rand.nextInt(links_i.size());
            int j = links_i.get(r);
            links.get(i).add(j);

            links_i.remove(r);
            ArrayList<Integer> links_j = neighbor.get(j);
            links_j.remove(links_j.indexOf(i));
        }
        for(int i=0; i<Constants.N; i++) { // randomly choose in-coming link for each node
            ArrayList<Integer> links_i = neighbor.get(i);
            if( links_i.size() == 0 ) { throw new RuntimeException("invalid network"); }
            int r = rand.nextInt(links_i.size());
            int j = links_i.get(r);
            links.get(j).add(i);

            links_i.remove(r);
            ArrayList<Integer> links_j = neighbor.get(j);
            links_j.remove(links_j.indexOf(i));
        }
        for(int i=0; i<Constants.N; i++) { // randomly choose in-coming link for each node
            ArrayList<Integer> links_i = neighbor.get(i);
            while( links_i.size() > 0 ) {
                int j = links_i.remove(0);
                if (rand.nextDouble() <= 0.5) {
                    links.get(j).add(i);
                } else {
                    links.get(i).add(j);
                }
                ArrayList<Integer> links_j = neighbor.get(j);
                links_j.remove(links_j.indexOf(i));
            }
        }

        return links;
    }

    public static double[] CalculateExpectedReturn(MarketAsset market, Random rand) {
        // [TODO] 実装 & calculate_expected... の削除
        double[] expected_return = new double[(int) Constants.N];
        ArrayList<Double> fundamental_price = market.getFundamental_price();        //理論価格の取得
        ArrayList<Double> market_price = market.getMarketPrice();    //市場価格の取得
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

    public boolean judgeVaR(MarketAsset market) {
        // [TODO] ... remove judge_VaR completed
        Boolean varjudge = false;
        double var = calculate_VaR(market);
        double VaRf = this.bs.EquityCapitalRatio(var);

        if(VaRf >= Constants.VaR.Threshold){
            varjudge = true;
        }
        return varjudge;
    }

    public static double calculate_VaR(MarketAsset market){
        ArrayList<Double> log_return_List = new ArrayList<Double>();

        ArrayList<Double> price = market.getMarketPrice();
        for(int j = price.size() - 1; j > price.size() - Constants.VaR.tp - 1; j--) {
            log_return_List.add(Math.log10(price.get(j) / price.get(j - 1)));
        }
        for(int j = 0; j < log_return_List.size(); j++){
            Collections.sort(log_return_List);
        }
        return Math.pow(10, log_return_List.get(Constants.VaR.threshold_under5));
    }

    //☆VaR制約→売り買いで「買い」＝＋１、「売り」＝−１を返す。
    public int BuyOrSell(MarketAsset market, Random rand){
        int plus_or_minus = 0;
            if(this.status){
                    if(!this.judgeVaR(market)){
                        plus_or_minus = -1;
                    }else{
                        if(judge_FCN(market, rand).get(this.index)){
                            plus_or_minus = 1;
                        }else{
                            plus_or_minus = -1;
                        }
                    }
            }
        return plus_or_minus;
    }
    public static ArrayList<Boolean> judge_FCN(MarketAsset market, Random rand){
        ArrayList<Boolean> each_judge_fcn = new ArrayList<Boolean>();		//結果を格納する配列

        for(int i = 0; i < Constants.N; i++){
            boolean judge_fcn = false;
            double [] e_returns = CalculateExpectedReturn(market, rand);
            if(e_returns[i] >= 0){
                judge_fcn = true;
            }
            each_judge_fcn.add(judge_fcn);
        }
        return each_judge_fcn;
    }

    public static void GoEachBankrupt(ArrayList<Bank> banks, MarketAsset market) {
        System.out.print("倒産したのは: ");

        ArrayList<Integer> bankrupted = new ArrayList<>();
        for (int i = 0; i < Constants.N; i++) {
            if (banks.get(i).status == false) {
                continue;
            }
            //CAR<ThresholdまたはGap<0の時に銀行は倒産
            if (!banks.get(i).judgeVaR(market)) {
                System.out.print(i + ", ");
                bankrupted.add(i);
            }
        }

        for (Integer i : bankrupted) {
            GoBankrupt(banks, i);
        }
        System.out.println();
    }

    public static void GoBankrupt(ArrayList<Bank> banks, int ruptID){
        banks.get(ruptID).status = false;		//状態を１→０に変える

        //貸し借り表の値を全て０にする
        Bank bi = banks.get(ruptID);
        for(Map.Entry<Integer,Double> entry: bi.List_borrowing.entrySet()) {
            int j = entry.getKey();
            entry.setValue(0.0);
            //倒産した銀行に貸出していた銀行のLendListを０にする
            Bank bj = banks.get(j);
            bj.List_lending.put(ruptID, 0.0);
        }
        for(Map.Entry<Integer,Double> entry: bi.List_lending.entrySet()) {
            int j = entry.getKey();
            entry.setValue(0.0);
            //倒産した銀行に貸出していた銀行のLendListを０にする
            Bank bj = banks.get(j);
            bj.List_borrowing.put(ruptID, 0.0);
        }
        //BSの値を全て０にする
        //BalanceSheet.OutputBalanceSheet(banks);
        banks.get(ruptID).bs.Clear();
    }

    public static void OutputNetwork(ArrayList<Bank> banks){
        for (int i = 0; i < Constants.N; i++){
            for(Map.Entry<Integer,Double> entry: banks.get(i).List_lending.entrySet()) {
                System.out.println(i + " " + entry.getKey());
            }
        }
    }

    public void InitializeBalanceSheet(ArrayList<Bank> banks, double sum_marketable_assets, MarketAsset market, Random rand) {
        this.bs.Initialize( List_lending.size(), banks, sum_marketable_assets, market, rand);
    }

    public void UpdateBalanceSheet(double latest_market_price) {
        if(status){
            double e_update = CountUpNumStocks() * latest_market_price / Constants.VaR.stockmulti;
            bs.marketable_asset = e_update;	//外部資産はBS(8)：持ち株数 * Mp（最新時刻）：市場価格から算出

            double l_update = 0.0;
            for(Map.Entry<Integer,Double> entry: List_lending.entrySet()) {
                l_update += entry.getValue(); //貸出額はLendListの総和から算出
            }
            bs.lending_money = l_update;

            double b_update = 0.0;
            for(Map.Entry<Integer,Double> entry: List_borrowing.entrySet()) {
                b_update += entry.getValue(); //貸出額はLendListの総和から算出
            }
            bs.borrowing_money = b_update;

            double gap = -(bs.cash + bs.marketable_asset + bs.lending_money
                    - bs.equity_capital - bs.account - bs.borrowing_money);
            double c_update = bs.equity_capital - gap + 0.0001;   //0.0001は浮動小数点対策

            bs.equity_capital = c_update;

            double a_update = Math.max(bs.cash + bs.marketable_asset + bs.lending_money,
                    bs.equity_capital + bs.account + bs.borrowing_money);	//資産a=max(外部資産e+銀行間貸出l, 自己資本c+預金d+銀行間借入b)
            bs.asset_sum = a_update;
        }
    }

    public int CountUpNumStocks(){
        return bs.num_stocks;
    }

    public void OutputBalanceSheet(MarketAsset market) {
        bs.OutputBalanceSheet(market);
    }
}

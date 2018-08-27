package RyoikiHamawaki;
import x10.util.*;
import x10.array.*;
import x10.lang.*;
import RyoikiHamawaki.Bank;
import RyoikiHamawaki.MarketAsset;
import plham.util.RandomHelper;


public class Main {
	

	static val m:Long = 100;					//VaRの計算に利用
	static val tp:Long = 60;					//VaRの計算に利用・過去何日分のデータを取ってくるか
	static val r_f:Double = 0.02;					//リスクフリーレート
	static val delta_t:Double = 0.004;				//Δt
	static val sigma:Double = 0.25;					//ボラティリティ
	static val M:Long = 1;						//外部資産数

	static val Threshold = 0.04;					//VaR制約の閾値
	static val ThresholdF = 0.08;					//資金繰りの時のVaR制約の閾値
	static val stockmulti = 20;					//株価を何倍に換算するか　＝　外部資産の与える影響の制御
	static val Control = 0.2 * stockmulti / 20;			//VaR制約の任意の係数

	

	static val N:Double = 100;                                          //銀行の数
	static val NLong:Long = 100;
	static val BigN:Double = 10;                                        //大銀行の数
	static val BigNLong:Long = 10;
	static val SmallN:Double = N - BigN;                                //小銀行の数

/*	static val N:Double = 12;                                          //銀行の数
	static val NLong:Long = 12;
	static val BigN:Double = 4;                                        //大銀行の数
	static val BigNLong:Long = 4;
	static val SmallN:Double = N - BigN;                                //小銀行の数

*/
	static val divnum = 10;						//何分割にして他の銀行から資金繰りするか

	static val time:Long = 100;
	static val rupttime:Long = 1;
	
	

	public static def main(args:Rail[String]) {



	var hnum:Long = 1;
	hnum = Long.parseLong(args(1));

	for(var h:Long = 0; h < hnum; h++){
		val rand:Random = new Random(h+1);				//randを固定する
		//Console.OUT.println(args(0));
	        
		//outputをコンパイル後に変更できるように
		var output:Boolean = true;			//出力をやめる/する
		if(Long.parseLong(args(0)) == 0){
			output = false;
		}

		val pricecontrol = Double.parseDouble(args(3));
		
		
		var bankArray:ArrayList[Bank] = new ArrayList[Bank]();       //銀行を入れる配列
		var X:Double = 2;                                            //繋がる銀行の数の期待値
		val p:Double = X/N;                                          //繋がる銀行の確率

		
		//val randA = new Random(16);
		var r:Double = rand.nextDouble();						//指数r,銀行間での貸借額の偏りの大きさを決めるパラメータ（ランダム）
		var firstE:Double = (130 + (20 * r )) * N;					//すべての銀行のバランスシートの外部資産の総額

		//各銀行のID/状態（0=倒産、1=倒産していない）を決定
		for(var i:Long=0;i<N;i++){
	        	bankArray(i) = new Bank(i,1);	
		}

		
		var bsp:Double = 0.0;
		bsp = Double.parseDouble(args(4));
		var numche:Long = Long.parseLong(args(5));
		var LinkNum:Long = Long.parseLong(args(6));
		var LinkNumB:Double = Double.parseDouble(args(7));
		var LinkNumS:Double = Double.parseDouble(args(8));

		//銀行間ネットワークの作成
		while(Bank.checkNet(bankArray)){
			Bank.makeNetwork(bankArray, rand, numche, LinkNum, LinkNumB, LinkNumS);
			Bank.makeVector(bankArray, rand, bsp);
		}
		//NeighborOutからNeighborInを作成
		for(var i:Long = 0; i < N; i++){
			for(var j:Long = 0; j < bankArray(i).NeighborOut.size(); j++){
				bankArray(bankArray(i).NeighborOut(j)).NeighborIn.add(i);
			}
		}


													//大小大の個数の確認
													var numberBSB:Double = Double.parseDouble(BSBcount(bankArray).toString());
													var numberBSBvector:Double = Double.parseDouble(BSBvectorcount(bankArray).toString());
													//Console.OUT.println("大小大の個数は" + numberBSB);
													//Console.OUT.println("大→小→大の個数は" + numberBSBvector);
													//Console.OUT.print(numberBSBvector / numberBSB + "  ");

													//ネットワークのグラフを出力する
													if(output){
														graph(bankArray);
													}
		
		//ωの作成
		Bank.makeOmg(bankArray, firstE, rand);

		//外部資産市場の作成
		val marketAssets = createMarketAssets(rand);

		//BSの作成
		Bank.makeBS(bankArray, firstE, marketAssets, rand);


													if(output){
														for(var i:Long = 0; i < N; i++){
															if(i % 10 == 0){
																Console.OUT.println();
															}
															//Console.OUT.print(bankArray(i).Gap + "  ");	
														}
														Console.OUT.println();	//バランスシートの確認(全部出力)
														for(var i:Long=0; i < N; i++){
															for(var j:Long=0; j < 10; j++){
																Console.OUT.println("BS(" + i +"," + j + "):" + bankArray(i).BS(j) + "  ");
															}
															Console.OUT.println();
														}
													}









		//VaR制約の判定(true OR false)
		VaRJudge(bankArray, marketAssets, Threshold);

		//期待リターンを求める
		Bank.makeExpReturn(bankArray, marketAssets, rand);

		//売り買いの判定
		val FCNjudgefirst = FCNJudge(bankArray);
		var FCNjudge:ArrayList[Boolean] = FCNjudgefirst;

		//買いなら＋１、売りなら−１を返す
		val BORSfirst = BorS(bankArray);
		var BORS:ArrayList[Long] = BORSfirst;
		
		val FuPr = marketAssets(0).getPrice();
		val MaPr = marketAssets(0).getMarketPrice();


		
		//貸し借りリストを作る
		Bank.BandLList(bankArray, firstE);
												


		var ruptnumfirst:Long = 0;
		var ruptnumlast:Long = 0;

		var ruptnum:Long =0;

		

		


		for(var t:Long = 0; t <= time; t++){
													if(output){
														Console.OUT.println();
														Console.OUT.println("時刻ｔ＝" + t +";");
													
														for(var i:Long = 0; i < N; i++){
															for(var j:Long = 0; j < bankArray(i).Neighbor.size(); j++){
																//Console.OUT.print(bankArray(i).Neighbor(j) + " ");
															}
															//Console.OUT.println();
														}
													}
			if(t > 0){
				if(t == rupttime){
					val ruptnumber = Long.parseLong(args(2));
					bankrupt(bankArray, ruptnumber);		//ｔ＝1で銀行潰す
				}
												
				updateBS(bankArray, marketAssets);	//バランスシートの更新
				makeGap(bankArray);			//Gapの更新
				
													if(output){
														//各期の資金ギャップの確認
														Console.OUT.println();
														//Console.OUT.println("資金ギャップ");
														for(var i:Long = 0; i < N; i++){
															if(i % 10 == 0){
																//Console.OUT.println();
															}
															//Console.OUT.print(bankArray(i).Gap + "  ");	
														}
														//Console.OUT.println();
													}
				//Financing(bankArray, marketAssets);	//資金繰りを行う
													if(output){
														//バランスシートの確認(全部出力)
														for(var i:Long=0; i < N; i++){
															for(var j:Long=0; j < 10; j++){
																Console.OUT.println("BS(" + i +"," + j + "):" + bankArray(i).BS(j) + "  ");

																	
																//Console.OUT.println("自己資本" + i + ":" + bankArray(i).BS(3));	
																//onsole.OUT.println("持株数" + i + ":" + bankArray(i).BS(8));	
															
																}
															var VaRf:Double = Control * bankArray(i).BS(3)/(Math.abs(bankArray(i).BS(8)) * 0.99);
															Console.OUT.println(VaRf);
															Console.OUT.println();
														}
													}
				//資金繰りを行う時用の更新部分
				//updateBS(bankArray, marketAssets);	//バランスシートの更新
				//makeGap(bankArray);			//Gapの更新
													if(output){
														//各期の資金ギャップの確認

														//Console.OUT.println();
														//Console.OUT.println("資金繰り後");
														for(var i:Long = 0; i < N; i++){
															if(i % 10 == 0){
																//Console.OUT.println();
															}
															//Console.OUT.print(bankArray(i).Gap + "  ");	
														}
														//Console.OUT.println();
													}

				VaRJudge(bankArray, marketAssets, Threshold);	//VaRのboolean更新
				Bankrupt(bankArray, output);			//銀行を倒産させる
				
				
			}

			Bank.makeExpReturn(bankArray, marketAssets, rand);	//期待リターンを求める
			FCNjudge = FCNJudge(bankArray);			//FCNエージェントのboolean更新
			VaRJudge(bankArray, marketAssets, Threshold);	//VaRのboolean更新
			BORS = BorS(bankArray);				//買いなら＋１、売りなら−１を返す更新


												if(output){			
													//各期の銀行の状態の確認
													for(var i:Long = 0; i < N; i++){
														if(i % 10 == 0 && i <= 10){
															Console.OUT.println();
														}
														if(i % 9 == 1 && i > 10){
															Console.OUT.println();
														} 
														Console.OUT.print(bankArray(i).status + "  ");
													}
													Console.OUT.println();

													//各期の売り買いのjudgeの確認
													for(var i:Long = 0; i < N; i++){
														if(i % 10 == 0){
															//Console.OUT.println();
														}
														//Console.OUT.print(bankArray(i).VaRjudgement(0)+"  " + FCNjudge(i) + "  " + BORS(i) + "; ");
													}
													//Console.OUT.println();
/*
													//borrowlist lendlist の確認
													Console.OUT.println("BList");
													for(var i:Long = 0; i<N; i++){
														Console.OUT.print(i + ": ");
														for(var j:Long = 0; j<N; j++){
															var bo:Double = bankArray(i).BorrowList.get(j);
															Console.OUT.print(bo + " ");
														}
														Console.OUT.println();
													}
													Console.OUT.println("LList");
													for(var i:Long = 0; i<N; i++){
														Console.OUT.print(i + ": ");
														for(var j:Long = 0; j < N; j++){
															var le:Double = bankArray(i).LendList.get(j);
															Console.OUT.print(le + " ");
														}
														Console.OUT.println();
													}
												
*/												}
			dealEA(bankArray, marketAssets, rand);			//外部資産を売買

												if(output){
													//これまでの価格を出力
													//Console.OUT.println("市場価格：");
													for(var i:Long = 0; i < MaPr.size(); i++){
														if(i % 10 == 0){
															Console.OUT.println();
														}
														Console.OUT.println(1 + " " + (i- 100) + " " + 0 + " " + 0 + " " + MaPr(i));
													}
													Console.OUT.println();

													//Console.OUT.println("理論価格：");
													for(var i:Long = 0; i < FuPr.size(); i++){
														if(i % 10 == 0){
															//Console.OUT.println();
														}
														//Console.OUT.print(FuPr(i) + "  ");
													}
													//Console.OUT.println();
												}


			updateMP(bankArray, marketAssets, pricecontrol);	//市場価格の更新
			updateFP(bankArray, marketAssets, rand);			//理論価格の更新

													//ファンダメンタルの確認用
													if(false){
														if(t < (time / 4)){
															updateFP100(bankArray, marketAssets, rand);			//理論価格の更新
														}else{
															updateFP80(bankArray, marketAssets, rand);			//理論価格の更新
														}
													}
												if(output){
													//VaR制約のtrue,falseの出力と個数を数える
													var countBnum:Long = 0;
													var countSnum:Long = 0;
													for(var i:Long = 0; i < N; i++){
														//Console.OUT.print(bankArray(i).VaRjudgement(0) + "  ");
														if(BORS(i) == 1){
															countBnum++;
														}
														if(BORS(i) == -1){
															countSnum++;
														}
													}
													//Console.OUT.println();
		
													Console.OUT.println("買いの数は"  + (countBnum) + "個");
													Console.OUT.println("売りの数は"  + (countSnum) + "個");
													for(var i:Long=0; i < N; i++){
														//Console.OUT.println("EA" + i + ":" + bankArray(i).BS(1));
				
													}
													for(var i:Long=0; i < N; i++){
														//Console.OUT.println("EAnumber" + i + ":" + bankArray(i).BS(8));
				
													}
												}

			
			if(t == (rupttime-1)){
				ruptnumfirst = countrupt(bankArray);
			}
			if(t == 100){
				ruptnumlast = countrupt(bankArray);
				ruptnum = ruptnumlast - ruptnumfirst;
			}
			
													//倒産数と価格の出力
													if(t == 100){
														priceplot(marketAssets);
														//Console.OUT.print(ruptnumfirst + "  ");
														//Console.OUT.print(ruptnumlast + "  ");
														Console.OUT.println(ruptnum);
														//Console.OUT.println();
													}			
		}


		




		//以下、確認用の出力
/*
		Console.OUT.println("0の借入＝"+bankArray(0).Borrowing);
		Console.OUT.println("0の貸出＝"+bankArray(0).Lending);
		Console.OUT.println("1の借入＝"+bankArray(1).Borrowing);
		Console.OUT.println("1の貸出＝"+bankArray(1).Lending);
		Console.OUT.println("2の借入＝"+bankArray(2).Borrowing);
		Console.OUT.println("2の貸出＝"+bankArray(2).Lending);	

		var amount:Double = 10;
		Bank.rent(bankArray(0),bankArray(1), amount);

		Console.OUT.println("取引後");
		Console.OUT.println("0の借入＝"+bankArray(0).Borrowing);
		Console.OUT.println("0の貸出＝"+bankArray(0).Lending);
		Console.OUT.println("1の借入＝"+bankArray(1).Borrowing);
		Console.OUT.println("1の貸出＝"+bankArray(1).Lending);
		Console.OUT.println("2の借入＝"+bankArray(2).Borrowing);
		Console.OUT.println("2の貸出＝"+bankArray(2).Lending);

		
		var countB:Long=0;
		var countL:Long=0;

		//銀行間ネットワークの確認
		Console.OUT.println("向きなし");
		for(var i:Long=0;i<N;i++){
			for(var j:Long=0;j<bankArray(i).Neighbor.size();j++){
				Console.OUT.print(bankArray(i).Neighbor(j) + " ");
			}
			Console.OUT.println();
		}


		Console.OUT.println("借入先");
		for(var i:Long=0;i<N;i++){
			for(var j:Long=0;j<bankArray(i).NeighborBor.size();j++){
				Console.OUT.print(bankArray(i).NeighborBor(j) + " ");
				countB++;
			}
			Console.OUT.println();
		}
		Console.OUT.println("貸出先");
		for(var i:Long=0;i<N;i++){
			for(var j:Long=0;j<bankArray(i).NeighborLen.size();j++){
				Console.OUT.print(bankArray(i).NeighborLen(j) + " ");
				countL++;
			}
			Console.OUT.println();
		}

		//ωの確認
		Console.OUT.println("omega");
		var sumO:Double = 0;
		for(var i:Long = 0; i<N; i++){
			for(var j:Long = 0; j<bankArray(i).Neighbor.size(); j++){
				var H:Double = bankArray(i).Omg.get(bankArray(i).Neighbor(j));
				sumO += H;

				Console.OUT.print(H + " ");
			}
			Console.OUT.println();
		}

		Console.OUT.println(countB);
		Console.OUT.println(countL);

		//ｋoutの本数の確認
		var sumkoutB:Double = 0;
		var sumkoutS:Double = 0;
		Console.OUT.println("kout");
		for(var i:Long = 0; i < N; i++){
			Console.OUT.println(bankArray(i).count);
			if(i < 10){
				sumkoutB += bankArray(i).count;
			}else{
				sumkoutS += bankArray(i).count;
			}
		}
		//Console.OUT.println(bankArray(0).count);
		Console.OUT.println("大銀行平均：" + (sumkoutB / 10));
		Console.OUT.println("小銀行平均：" + (sumkoutS / 90));
		Console.OUT.println();
		Console.OUT.println();
		Console.OUT.println();
		
		//バランスシート（０資産、１外部資産、２貸出額、３自己資本、４預金、５借入額、６銀行間貸出比率、７自己資本比率）の確認
		//var suml:Double = 0;
		for(var i:Long=0; i < N; i++){
				Console.OUT.println("EAnumber" + i + ":" + bankArray(i).BS(8));
				//suml += bankArray(i).BS(1);
		}
		//Console.OUT.println(suml);
		//Console.OUT.println(sumO);


		//バランスシート（０資産、１外部資産、２貸出額、３自己資本、４預金、５借入額、６銀行間貸出比率、７自己資本比率）の確認
			for(var i:Long=0; i < N; i++){	
				//Console.OUT.println("EA" + i + ":" + bankArray(i).BS(1));	
			}
			for(var i:Long=0; i < N; i++){	
				//Console.OUT.println("貸出" + i + ":" + bankArray(i).BS(2));	
			}
			for(var i:Long=0; i < N; i++){	
				//Console.OUT.println("自己資本" + i + ":" + bankArray(i).BS(3));	
			}
			for(var i:Long=0; i < N; i++){	
				//Console.OUT.println("借入" + i + ":" + bankArray(i).BS(5));	
			}
			for(var i:Long=0; i < N; i++){	
				//Console.OUT.println("持株数" + i + ":" + bankArray(i).BS(8));	
			}



		//外部資産を更新
		updateEA(bankArray, marketAssets);

		//バランスシートの確認(全部出力)
		for(var i:Long=0; i < N; i++){
			for(var j:Long=0; j < 9; j++){
				Console.OUT.print("BS(" + i +"," + j + "):" + bankArray(i).BS(j) + "  ");
			}
			Console.OUT.println();
		}
		


		//VaRの確認
		val logreturn = calVaR(marketAssets);
		for(var i:Long = 0; i < logreturn.size(); i++){
			Console.OUT.println(i + "  " + logreturn(i));
		}

		//VaR制約のtrue,falseの出力と個数を数える
		var countVaR:Long = 0;
		for(var i:Long = 0; i < N; i++){
			Console.OUT.print(bankArray(i).VaRjudgement(0) + "  ");
			if(bankArray(i).VaRjudgement(0) == true){
				countVaR++;
			}
		}
		
		Console.OUT.println("trueの数は" + countVaR + "個");
		Console.OUT.println("falseの数" + (100 - countVaR) + "個");


		//marketAssetの動作確認
		Console.OUT.println(marketAssets(0).getR_avg());
		Console.OUT.println(marketAssets(0).getSigma_m());
		val test1 = marketAssets(0).getPrice();
		val markettest2 = marketAssets(0).getMarketPrice();
		for(var i:Long = 0; i <= m; i++){
			Console.OUT.print(test1(i) + " ");
			if(i % 10 == 0){
				Console.OUT.println();
			}
		}
		Console.OUT.println();
		Console.OUT.println("VaR ＝ " + VaR(0));
		//Console.OUT.println(test2(1));
		for(var i:Long = 0; i <= m; i++){
			Console.OUT.print(markettest2(i) + " ");
			if(i % 10 == 0){
				Console.OUT.println();
			}
		}
		
		//時刻ｔ＝０の価格を確認
		val pricetest = marketAssets(0).getPrice();
		val Marketpricetest = marketAssets(0).getMarketPrice();
		Console.OUT.println(pricetest(m));
		Console.OUT.println(Marketpricetest(m));

		//期待リターンの確認
		for(var i:Long = 0; i < N; i++){
			Console.OUT.println(bankArray(i).ExpReturn);
		}


		//買いか売りかで１，ー１を返す確認
		for(var i:Long = 0; i < N; i++){
			if(i % 10 == 0){
				Console.OUT.println();
			}
			Console.OUT.print(BORS(i) + "  ");
		}

		//borrowlist lendlist の確認
		Console.OUT.println("BList");
		for(var i:Long = 0; i<N; i++){
			Console.OUT.print(i + ": ");
			for(var j:Long = 0; j<bankArray(i).Neighbor.size(); j++){
				var bo:Double = bankArray(i).BorrowList.get(bankArray(i).Neighbor(j));
				Console.OUT.print(bo + " ");
			}
			Console.OUT.println();
		}
		Console.OUT.println("LList");
		for(var i:Long = 0; i<N; i++){
			Console.OUT.print(i + ": ");
			for(var j:Long = 0; j<bankArray(i).Neighbor.size(); j++){
				var le:Double = bankArray(i).LendList.get(bankArray(i).Neighbor(j));
				Console.OUT.print(le + " ");
			}
			Console.OUT.println();
		}


		//borrowlist lendlist の確認
			Console.OUT.println("BList");
			for(var i:Long = 0; i<N; i++){
				Console.OUT.print(i + ": ");
				for(var j:Long = 0; j<bankArray(i).NeighborOut.size(); j++){
					var bo:Double = bankArray(i).BorrowList.get(bankArray(i).NeighborOut(j));
					Console.OUT.print(bo + " ");
				}
				Console.OUT.println();
			}
			Console.OUT.println("LList");
			for(var i:Long = 0; i<N; i++){
				Console.OUT.print(i + ": ");
				for(var j:Long = 0; j < bankArray(i).NeighborOut.size(); j++){
					var le:Double = bankArray(bankArray(i).NeighborOut(j)).LendList.get(i);
					Console.OUT.print(le + " ");
				}
				Console.OUT.println();
			}

		
		//resultを作り、グラフを作るための出力
		for(var i:Long = 0; i < 100 + time; i++){
			Console.OUT.print(1 + "  ");
			Console.OUT.print((i - 99) + "  ");
			Console.OUT.print(0 + "  ");
			Console.OUT.print(0 + "  ");
			Console.OUT.print(MaPr(i) + "  ");
			Console.OUT.println(FuPr(i));
		}


		//shuffleの確認
		var a:ArrayList[Long] = new ArrayList[Long]();
		val randshuffle = new Random();
		
		for(var i:Long = 0; i < 10; i++){
			a(i) = i;
		}

		

		for(var i:Long = 0; i < 10; i++){
			Console.OUT.print(a(i) + "  ");
		}
		Console.OUT.println();

		shuffle(a);

		for(var i:Long = 0; i < 10; i++){
			Console.OUT.print(a(i) + "  ");
		}
		Console.OUT.println();

*/
	//testarea
		//priceplot(marketAssets);
	}

	}//public static def main(args:Rail[String])の終わりのカッコ



	/*以下、createMarketAssets(),setupMarketAsset(marketAsset:MarketAsset, number:Long)は、
	曽根さんの/home/hamawakiryo/Plham/GraduationResearchより引用
	外部資産エージェントを生成する*/
	public static def createMarketAssets(rand:Random):List[MarketAsset]{
		val marketAssets = new ArrayList[MarketAsset]();	//外部資産エージェントを格納する配列を生成
		for(var j:Long=0;j<M;j++){
			val marketAsset = new MarketAsset();
			setupMarketAsset(marketAsset, j, rand);
			marketAssets.add(marketAsset);
		}
		return marketAssets;
	}

	//外部資産エージェントに各変数の初期値を与える
	//とりあえず、１０万個作る。一番最低なやつを取ってくる。 →　今回はこの手法は用いない


	public static def setupMarketAsset(marketAsset:MarketAsset, number:Long ,rand:Random){
		var priceSimulations:ArrayList[ArrayList[Double]] = new ArrayList[ArrayList[Double]]();		//10万通りの過去(m+1)日間における価格を格納する箱
		//val rand2 = new Random();
		val randHelp = new RandomHelper(rand);
		for(var i:Long=0;i<1;i++){
			var priceSimulation:ArrayList[Double] = new ArrayList[Double]();			//過去(m+1)日間における理論価格（＝市場価格）
			var p:Double = 100.0;
			priceSimulation.add(p);
			for(var j:Long=1;j<m+1;j++){
				p = p+r_f*p*delta_t+sigma*p*randHelp.nextNormal(0.0,1.0)*Math.sqrt(delta_t);	//確率差分方程式で理論価格を計算
				priceSimulation.add(p);								//第i試行の過去(m+1)日間の価格を加えていく
			}
			priceSimulations.add(priceSimulation);
		}
		var price:ArrayList[Double] = new ArrayList[Double]();						//過去(m+1)日間におけ価格
		price = priceSimulations.operator()(0);								//まずは、第0試行を採用
		for(var i:Long=1;i<1;i++){
			if(priceSimulations.operator()(i).operator()(m)<price.operator()(m)){
				price = priceSimulations.operator()(i);						//最新価格が最低となっている価格時系列を採用
			}
		}

		//市場価格の作成
		var start:Boolean = false;					//市場価格を計算し始める
		var MarketPrice:ArrayList[Double] = new ArrayList[Double]();
		if(!start){
			for(var i:Long = 0; i < price.size(); i++){
				MarketPrice(i) = price(i);
			}
		}else{
			
		}



		//価格の初期値
		var firstPrice:Double = price.operator()(m);							//t=0における価格（ｔ＝0における時価）を外部資産の価格の初期値とする。
		//平均リターン
		var r_avg:Double = 0.0;	//平均リターン
		var num1:Long=0;	//カウンター
		for(var k:Long=0;k<m;k++){
			if(price.operator()(k)==0.0) continue;							//例外処理。価格が0はスルー。
			r_avg += (price.operator()(k+1)/price.operator()(k));					//過去m日間の日次リターンを足しあわせていき、
			num1++;
		}
		if(num1==0){
			r_avg = 0.0;	//価格が0ばかりのときは、平均リターンも0。
		}else{
			r_avg = r_avg/num1;	//その平均を求める。
		}
		//標準偏差
		var sigma_m:Double = 0.0;	//標準偏差
		var num2:Long = 0;	//カウンター
		for(var k:Long=0;k<m;k++){
			if(price.operator()(k)==0.0) continue;	//例外処理。価格が0はスルー。
			sigma_m += ((price.operator()(k+1)/price.operator()(k))-r_avg)*((price.operator()(k+1)/price.operator()(k))-r_avg);	//過去m日間の平均リターン周りの二次モーメントを足しあわせていき、
			num2++;
		}
		if(num2==0){
			sigma_m = 0.0; //例外処理。価格0ばかりのときは、標準偏差も0とする。
		}else{
			sigma_m = Math.sqrt(sigma_m/num2);		//その平均（分散）の平方根を求める
		}
		marketAsset.setFirstPrice(firstPrice);
		marketAsset.setPrice(price);
		marketAsset.setMarketPrice(MarketPrice);
		marketAsset.setR_avg(r_avg);
		marketAsset.setSigma_m(sigma_m);
	}

	//☆VaR関連の更新行い、買いか売りかを更新する
/*	public static def updateBorS(bankArray:ArrayList[Bank], marketAssets:List[MarketAsset]):ArrayList[Long]{
		VaRJudge(bankArray, marketAssets, Threshold);
		Bank.makeExpReturn(bankArray, marketAssets);
		FCNJudge(bankArray);
		val BORS:ArrayList[Long] = BorS(bankArray);

		return BORS;
	}
*/

	//☆VaRの算出を行う
	public static def calVaR(marketAssets:List[MarketAsset]):ArrayList[Double]{
		var VaRList:ArrayList[Double] = new ArrayList[Double]();
		var logreturnList:ArrayList[Double] = new ArrayList[Double]();

		for(var i:Long = 0; i < M; i++){
			val price = marketAssets(i).getMarketPrice();

			//対数期待リターンの算出・格納
			for(var j:Long = price.size() - 1; j > price.size() - tp - 1; j--){
				logreturnList.add(Math.log10(price(j)/price(j-1)));
			}
			//配列のソート
			for(var j:Long = 0; j < logreturnList.size(); j++){
				logreturnList.sort();
			}
			var underline:Long = tp * 5 / 100;
			
			VaRList.add(Math.pow(10, logreturnList(underline)));
		}
		return VaRList;



/*正規分布近似で求めたVaR　←　今回は使わない
		var AvgList:ArrayList[Double] = new ArrayList[Double]();		//外部資産ごとの対数平均を格納する配列
		var SigList:ArrayList[Double] = new ArrayList[Double]();		//外部資産ごとの対数分散を格納する配列
		var VaRList:ArrayList[Double] = new ArrayList[Double]();		//外部資産ごとのVaRを格納する配列

		//対数平均の計算
		for(var i:Long = 0; i < M; i++){
			val price = marketAssets(i).getMarketPrice();
			var logreturnAvg:Double = 0;
			for(var j:Long = price.size() - 1; j > price.size() - tp; j--){
				logreturnAvg += Math.log10(price(j)/price(j-1));
			}
			logreturnAvg = logreturnAvg / tp;
			AvgList.add(logreturnAvg);
		}
		//対数分散の計算
		for(var i:Long = 0; i < M; i++){
			val price = marketAssets(i).getMarketPrice();
			var logreturnSig:Double = 0;
			for(var j:Long = price.size() - 1; j > price.size() - tp; j--){
				logreturnSig += Math.pow(Math.log10(price(j)/price(j-1))-AvgList(i),2);
			}
			logreturnSig = logreturnSig / tp;
			logreturnSig = Math.sqrt(logreturnSig);
			SigList.add(logreturnSig);
		}
		//P(Z<=-1.6449) = 0.05より、Z=（X-μ）/σを代入して、P(X<=μ-1.6449σ) = 0.05なので、
		for(var i:Long = 0; i < M; i++){
			VaRList(i) = AvgList(i) - (1.6449 * SigList(i));
			VaRList(i) = Math.pow(10 , VaRList(i));
		}
		return VaRList;
*/
	}

	//☆総資産/（最悪のケースの損害＝外部資産*(1-VaR)）*調整のパラメータC　<=　閾値　かどうかで、true/falseを返す。
	public static def VaRJudge(bankArray:ArrayList[Bank], marketAssets:List[MarketAsset], deadline:Double):void{
		val VaRList:ArrayList[Double] = calVaR(marketAssets);

		//不等式の作成
		for(var i:Long = 0; i < N; i++){
			var VaRjudge:ArrayList[Boolean] = new ArrayList[Boolean]();		//リターンする銀行ごとのBooleanの配列
			for(var j:Long = 0; j < M; j++){
				var varjudge:Boolean = false;
				var VaRf:Double = Control * bankArray(i).BS(3) / (Math.abs(bankArray(i).BS(8)) * VaRList(j));
				bankArray(i).BS(7) = VaRf;
				
				if(VaRf >= deadline){
					varjudge = true;
				}
				VaRjudge.add(varjudge);
			}
			bankArray(i).VaRjudgement = VaRjudge;
		}
	}

	//☆FCNエージェントが出す期待リターンに基づき、正ならばtrue/負ならばfalseを返す。
	public static def FCNJudge(bankArray:ArrayList[Bank]):ArrayList[Boolean]{
		var fcnjudge:ArrayList[Boolean] = new ArrayList[Boolean]();		//結果を格納する配列

		for(var i:Long = 0; i < N; i++){
			var judgefcn:Boolean = false;
			if(bankArray(i).ExpReturn >= 0){
				judgefcn = true;
			}
			fcnjudge.add(judgefcn);
		}
		return fcnjudge;
	}

	//☆VaR制約→売り買いで「買い」＝＋１、「売り」＝−１を返す。
	public static def BorS(bankArray:ArrayList[Bank]):ArrayList[Long]{
		var bors:ArrayList[Long] = new ArrayList[Long]();			//結果を格納する配列
		val fcnjudge = FCNJudge(bankArray);
		for(var i:Long = 0; i < N; i++){
			var PorM:Long = 0;
			if(bankArray(i).status != 0){


				if(PorM == 0){
					if(!bankArray(i).VaRjudgement(0)){
						PorM = -1;
					}else{
						if(fcnjudge(i)){
							PorM = 1;
						}else{
							PorM = -1;
						}
					}
				}

			}
			bors.add(PorM);
		}
		return(bors);
	}

	//☆投資ターンでのBSの外部資産の売買(１単位ずつしか売り買いせず、売りと買いをランダムにマッチング)
	public static def dealEA(bankArray:ArrayList[Bank], marketAssets:List[MarketAsset], rand:Random):void{
		val Mp = marketAssets(0).getMarketPrice();		//市場価格を取得
		val bors = BorS(bankArray);					//買いか売りかを取得
		var sum:Double = 0.0;						//買いと売りどちらが多いかを判定
		var plus:ArrayList[Long] = new ArrayList[Long]();		//買いの銀行のIDを格納
		var minus:ArrayList[Long] = new ArrayList[Long]();		//売りの銀行のIDを格納

		//val rand = new Random();

		for(var i:Long = 0; i < N; i++){
			sum += bors(i);
			if(bors(i) == 1){
				plus.add(i);
			}else if(bors(i) == -1){
				minus.add(i);
			}
		}
		for(var i:Long = 0; i < N; i++){
			//買い手の方が多い時
			if(sum > 0){
				if(bors(i) == -1){
					bankArray(i).BS(8)--;				//買い手が多かったら、売り手は絶対売れる
					bankArray(i).BS(9) += Mp(Mp.size() - 1) / stockmulti;	//現金が増える
					//Console.OUT.print("(" + i + ",");

					//買い手は売り手の数だけしか買えない　→　ランダムにminusの中から取ってくる
					val buyer = rand.nextLong(plus.size());
					bankArray(plus(buyer)).BS(8)++;				//購入
					bankArray(plus(buyer)).BS(9) -= Mp(Mp.size() - 1) / stockmulti;	//現金が減る
					//Console.OUT.print(plus(buyer) + "),");
					plus.remove(plus(buyer));			//一度買ったら除外
				}
			}//売り手の方が多い時
			else{
				if(bors(i) == 1){
					bankArray(i).BS(8)++;			//売り手が多かったら、買い手は絶対買える
					bankArray(i).BS(9) -= Mp(Mp.size() - 1) / stockmulti;	//現金が減る
					//Console.OUT.print("(" + i + ",");

					//売り手は買い手の数だけしか売れない　→　ランダムにplusの中から取ってくる
					val seller = rand.nextLong(minus.size());
					bankArray(minus(seller)).BS(8)--;		//売却
					bankArray(minus(seller)).BS(9) += Mp(Mp.size() - 1) / stockmulti;	//現金が増える
					//Console.OUT.print(minus(seller) + "),");
					minus.remove(minus(seller));			//一度売ったら除外
				}
			}
		}
		
	}

	//☆市場価格の更新をします。
	public static def updateMP(bankArray:ArrayList[Bank], marketAssets:List[MarketAsset], pricecontrol:Double):void{
		val Marketprice = marketAssets(0).getMarketPrice();		//市場価格を取得
		val BORS = BorS(bankArray);					//買いか売りかを取得
		var buysurplus:Double = 0.0;
		var number:Double = 0.0;

		//買いがどれだけ多いかを数える
		for(var i:Long = 0; i < N; i++){
			buysurplus += BORS(i);
		}

		//取引数を数える
		if(buysurplus > 0){
			number = (N - buysurplus) / 2;
		}else{
			number = (N + buysurplus) / 2;
		}

		//総株数を数える
		var sumofstock:Long = 0;
		for(var i:Long = 0; i < N; i++){
			sumofstock += bankArray(i).BS(8);
		}

		var newprice:Double = 0.0;					//新しい価格の初期化
		newprice = Marketprice(Marketprice.size()-1) + pricecontrol * (Marketprice(Marketprice.size()-1)  * buysurplus / (sumofstock));		//(Pn+1 - Pn) / Pn = α×(Nb -Ns)/[総株数]の計算
		Marketprice.add(newprice);
	}

	//☆理論価格の更新をします。
	public static def updateFP(bankArray:ArrayList[Bank], marketAssets:List[MarketAsset], rand:Random):void{


		val Fp = marketAssets(0).getPrice();
		//val rand2 = new Random();
		val randHelp = new RandomHelper(rand);

		var newprice:Double = 0.0;
		newprice = Fp(Fp.size()-1)+r_f*Fp(Fp.size()-1)*delta_t+sigma*Fp(Fp.size()-1)*randHelp.nextNormal(0.0,1.0)*Math.sqrt(delta_t);	//確率差分方程式で理論価格を計算
		Fp.add(newprice);
	}
	public static def updateFP100(bankArray:ArrayList[Bank], marketAssets:List[MarketAsset], rand:Random):void{


		val Fp = marketAssets(0).getPrice();
		//val rand2 = new Random();
		val randHelp = new RandomHelper(rand);

		var newprice:Double = 100.0;
/*		newprice = Fp(Fp.size()-1)+r_f*Fp(Fp.size()-1)*delta_t+sigma*Fp(Fp.size()-1)*randHelp.nextNormal(0.0,1.0)*Math.sqrt(delta_t);	//確率差分方程式で理論価格を計算
*/		Fp.add(newprice);
	}
	public static def updateFP80(bankArray:ArrayList[Bank], marketAssets:List[MarketAsset], rand:Random):void{


		val Fp = marketAssets(0).getPrice();
		//val rand2 = new Random();
		val randHelp = new RandomHelper(rand);

		var newprice:Double = 80.0;
/*		newprice = Fp(Fp.size()-1)+r_f*Fp(Fp.size()-1)*delta_t+sigma*Fp(Fp.size()-1)*randHelp.nextNormal(0.0,1.0)*Math.sqrt(delta_t);	//確率差分方程式で理論価格を計算
*/		Fp.add(newprice);
	}	



	//☆価格変動によるBSの外部資産と総資産の更新
	//★卒論でBSの更新変更
	public static def updateBS(bankArray:ArrayList[Bank], marketAssets:List[MarketAsset]):void{
		val Marketprice = marketAssets(0).getMarketPrice();
		//1.外部資産の更新
		for(var i:Long = 0; i < N; i++){
			if(bankArray(i).status == 0){
				continue;
			}
			bankArray(i).BS(1) = bankArray(i).BS(8) * Marketprice(Marketprice.size() - 1) / stockmulti;		//外部資産はBS(8)：持ち株数 * Mp（最新時刻）：市場価格から算出
			}

		//2.貸出の更新
		for(var i:Long = 0; i < N; i++){
			var UDl:Double = 0;
			for(var j:Long = 0; j < bankArray(i).NeighborOut.size(); j++){
				UDl += bankArray(i).LendList.get(bankArray(i).NeighborOut(j));	//貸出額はLendListの総和から算出
			}
			bankArray(i).BS(2) = UDl;
		}

		//5.借入額の更新
		for(var i:Long = 0; i < N; i++){
			var UDb:Double = 0;
			for(var j:Long = 0; j < bankArray(i).Neighbor.size(); j++){
				UDb += bankArray(i).BorrowList.get(bankArray(i).Neighbor(j));	//借入額はBorrowListの総和から算出
			}
			bankArray(i).BS.set(UDb,5);
		}

		//3.自己資本の更新
		for(var i:Long = 0; i < N; i++){
			var UDc:Double = 0;
			val gap = -(bankArray(i).BS(9) + bankArray(i).BS(1) + bankArray(i).BS(2)
						- bankArray(i).BS(3) - bankArray(i).BS(4) - bankArray(i).BS(5));
			UDc = bankArray(i).BS(3) - gap + 0.0001;   //0.0001は浮動小数点対策

			bankArray(i).BS.set(UDc, 3);
		}

		//0.総資産の更新
		for(var i:Long=0;i<NLong;i++){
			var a:Double=0.0;
			a = Math.max(bankArray(i).BS(9) + bankArray(i).BS(1) + bankArray(i).BS(2), 
							bankArray(i).BS(3) + bankArray(i).BS(4) + bankArray(i).BS(5));	//資産a=max(外部資産e+銀行間貸出l, 自己資本c+預金d+銀行間借入b)
			bankArray(i).BS.set(a,0);
		}

	}

	//☆資金ギャップを求める
	public static def makeGap(bankArray:ArrayList[Bank]):void{
		for(var i:Long = 0; i < N; i++){
			bankArray(i).Gap = -(bankArray(i).BS(9) + bankArray(i).BS(1) + bankArray(i).BS(2)
								- bankArray(i).BS(3) - bankArray(i).BS(4) - bankArray(i).BS(5));
		}
	}


	//☆資金繰りを行うなど、銀行を呼び出すときにシャッフルするためのメソッド
	public static def shuffle(neighbor:ArrayList[Long], rand:Random):void{//bookmark
		//val randshu = new Random();
		for(var i:Long = 0; i < neighbor.size(); i++){
			var j:Long = Math.abs(rand.nextLong()) % neighbor.size();
			var t:Long = neighbor(i);
			neighbor(i) = neighbor(j);
			neighbor(j) = t;
		}
	}

	//☆貸す量を制限するためのメソッド
	

	//☆資金繰りを行うメソッド
	public static def Financing(bankArray:ArrayList[Bank], marketAssets:List[MarketAsset], rand:Random):void{
		for(var i:Long = 0; i < N; i++){
			if(bankArray(i).status == 0){
				continue;
			}
			if(bankArray(i).Gap < 0){
				var borrowcounter:Long = 0;
	
				for(var j:Long = 0; j < bankArray(i).Neighbor.size(); j++){
					if(bankArray(bankArray(i).Neighbor(j)).Gap > 0){
						borrowcounter++;
					}
				}
				if(borrowcounter == 0){
					val moneyk = Math.abs(bankArray(i).Gap / divnum);
					//更に友達の友達を検索する
					for(var j:Long = 0; j < bankArray(i).Neighbor.size(); j++){
					if(bankArray(bankArray(i).Neighbor(j)).status == 0){
						continue;
					}
						for(var k:Long = 0; k < bankArray(bankArray(i).Neighbor(j)).Neighbor.size(); k++){
							if(bankArray(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)).Gap > 0){
	 							borrowcounter++;
							}
						}

						//実際に貸し借りを行っていく
						//借りる人がいない時
						if(borrowcounter == 0){
							Console.OUT.println("------------------------------------test0-----------------------------------------");
							continue;											//友達の友達でもダメな場合はcontinueにしておく
						}

						//友達の友達から借りられるとき
						else{
						//友達の友達から借りるときには、Neighborリストにいれて、新しく追加
							shuffle(bankArray(bankArray(i).Neighbor(j)).Neighbor, rand);		//銀行を呼び出す順番をランダムにする

							for(var k:Long = 0; k < bankArray(bankArray(i).Neighbor(j)).Neighbor.size(); k++){
							if(bankArray(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)).status == 0){
								continue;
							}
								if(bankArray(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)).Gap > 0){
									var counttest1:Long = 0;
									while(bankArray(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)).Gap > 5 * moneyk && bankArray(i).Gap < 0){
										if(counttest1 == 0){
											//BorrowListの更新
											bankArray(i).BorrowList.put(bankArray(bankArray(i).Neighbor(j)).Neighbor(k), moneyk);
		
											//LendListの更新
											bankArray(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)).LendList.put(i, moneyk);
									
											//Neighborの更新
											bankArray(i).memoryID.add(bankArray(bankArray(i).Neighbor(j)).Neighbor(k));
											bankArray(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)).memoryID.add(i);

											//VaRjudgementの更新
											updateBS(bankArray, marketAssets);
											makeGap(bankArray);
										

											Console.OUT.println(bankArray(i).BorrowList.get(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)));
											Console.OUT.println(bankArray(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)).LendList.get(i));
											Console.OUT.println("------------------------------------test1"+ bankArray(bankArray(i).Neighbor(j)).Neighbor(k) +"  →   "+i+"-----------------------------------------");

											bankArray(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)).memoryID.clear();
											counttest1++;
										}else{
											//BorrowListの更新
											val bbefore = bankArray(i).BorrowList.get(bankArray(bankArray(i).Neighbor(j)).Neighbor(k));
											val newbmoney = bbefore + moneyk;
											bankArray(i).BorrowList.remove(bankArray(bankArray(i).Neighbor(j)).Neighbor(k));
											bankArray(i).BorrowList.put(bankArray(bankArray(i).Neighbor(j)).Neighbor(k), newbmoney);

											//LendListの更新
											val lbefore = bankArray(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)).LendList.get(i);
											val newlmoney = lbefore + moneyk;
											bankArray(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)).LendList.remove(i);
											bankArray(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)).LendList.put(i, newlmoney);

											//Neighborの更新
											bankArray(i).memoryID.add(bankArray(bankArray(i).Neighbor(j)).Neighbor(k));
											bankArray(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)).memoryID.add(i);

											//VaRjudgementの更新
											updateBS(bankArray, marketAssets);
											makeGap(bankArray);
										

											Console.OUT.println(bankArray(i).BorrowList.get(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)));
											Console.OUT.println(bankArray(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)).LendList.get(i));
											Console.OUT.println("------------------------------------test1"+ bankArray(bankArray(i).Neighbor(j)).Neighbor(k) +"  →   "+i+"-----------------------------------------");

											bankArray(bankArray(bankArray(i).Neighbor(j)).Neighbor(k)).memoryID.clear();
											


										}
									}
								}
							}
						}
						
					}

					for(var j:Long = 0; j < bankArray(i).memoryID.size(); j++){
						bankArray(i).Neighbor.add(bankArray(i).memoryID(j));
						bankArray(bankArray(i).memoryID(j)).Neighbor.add(i);
						bankArray(i).memoryID.clear();
					}
				}

				//友達から借りられるとき
				else{
				//今までの友達から借りるときは、前の履歴を消して新しく追加
					val moneyj = Math.abs(bankArray(i).Gap / divnum);
					shuffle(bankArray(i).Neighbor, rand);		
					for(var j:Long = 0; j < bankArray(i).Neighbor.size(); j++){
					if(bankArray(bankArray(i).Neighbor(j)).status == 0){
						continue;
					}
						if(bankArray(bankArray(i).Neighbor(j)).Gap > 0){
							while(bankArray(bankArray(i).Neighbor(j)).Gap > 5 * moneyj && bankArray(i).Gap < 0){
								//BorrowListの更新
								val bbefore = bankArray(i).BorrowList.get(bankArray(i).Neighbor(j));
								val newbmoney = bbefore + moneyj;
								bankArray(i).BorrowList.remove(bankArray(i).Neighbor(j));
								bankArray(i).BorrowList.put(bankArray(i).Neighbor(j), newbmoney);

								//LendListの更新
								val lbefore = bankArray(bankArray(i).Neighbor(j)).LendList.get(i);
								val newlmoney = lbefore + moneyj;
								bankArray(bankArray(i).Neighbor(j)).LendList.remove(i);
								bankArray(bankArray(i).Neighbor(j)).LendList.put(i, newlmoney);


								Console.OUT.println(bankArray(i).BorrowList.get(bankArray(i).Neighbor(j)));
								Console.OUT.println(bankArray(bankArray(i).Neighbor(j)).LendList.get(i));
								Console.OUT.println("------------------------------------test2"+ bankArray(i).Neighbor(j)+"  →   "+i+"-----------------------------------------");

								updateBS(bankArray, marketAssets);
								makeGap(bankArray);

							}
						}
					}
				}

			}
		}//forのi

	}

	//☆倒産した銀行の処理
	public static def bankrupt(bankArray:ArrayList[Bank], ruptID:Long):void{
		bankArray(ruptID).status = 0;		//状態を１→０に変える

		//貸し借り表の値を全て０にする
		for(var i:Long = 0; i < bankArray(ruptID).NeighborIn.size(); i++){
			//BorrowListを０にする
			bankArray(ruptID).BorrowList.remove(bankArray(ruptID).NeighborIn(i));
			bankArray(ruptID).BorrowList.put(bankArray(ruptID).NeighborIn(i), 0);
			//倒産した銀行に貸出していた銀行のLendListを０にする
			bankArray(bankArray(ruptID).NeighborIn(i)).LendList.remove(ruptID);
			bankArray(bankArray(ruptID).NeighborIn(i)).LendList.put(ruptID, 0);
		}

		for(var i:Long = 0; i < bankArray(ruptID).NeighborOut.size(); i++){
			//LendListを０にする
			bankArray(ruptID).LendList.remove(bankArray(ruptID).NeighborOut(i));
			bankArray(ruptID).LendList.put(bankArray(ruptID).NeighborOut(i), 0);
			//倒産した銀行から借入れていた銀行のBorrowListを０にする
			bankArray(bankArray(ruptID).NeighborOut(i)).BorrowList.remove(ruptID);
			bankArray(bankArray(ruptID).NeighborOut(i)).BorrowList.put(ruptID, 0);
		}
		for(var i:Long = 0; i < bankArray(ruptID).Neighbor.size(); i++){
			//NeighborからruptIDを外す
			bankArray(bankArray(ruptID).Neighbor(i)).Neighbor.remove(ruptID);
		}	
			
		

		//BSの値を全て０にする
		for(var i:Long = 0; i < 10; i++){
			bankArray(ruptID).BS(i) = 0;		//外部資産はとりあえず売ったりしない
		}

	}
	//☆実際に倒産させるメソッド
	public static def Bankrupt(bankArray:ArrayList[Bank], outputb:Boolean):void{
		for(var i:Long = 0; i < N; i++){
			if(bankArray(i).status == 0){
				continue;
			}
			//CAR<ThresholdまたはGap<0の時に銀行は倒産
			if(!bankArray(i).VaRjudgement(0)){
				var VaRf:Double = Control * bankArray(i).BS(3) / (Math.abs(bankArray(i).BS(8)) * 0.98);
				
				bankrupt(bankArray, i);

				if(outputb){
					Console.OUT.println(i + "は自己資本比率悪化で倒産");
					Console.OUT.println(i + "の自己資本比率は" + VaRf);
				}
			}
			if(bankArray(i).Gap < 0){
				bankrupt(bankArray, i);

				if(outputb){
					Console.OUT.println(i + "はGap<0で倒産");
				}
			}
		}
	}	


	//☆ネットワークのグラフを書くための出力
	public static def graph(bankArray:ArrayList[Bank]):void{
		for(var i:Long = 0; i < N; i++){
			for(var j:Long = 0; j < bankArray(i).NeighborOut.size(); j++){
				Console.OUT.println(i + "  " + bankArray(i).NeighborOut(j));
			}
		}
	}

	//☆価格の変動を出力
	public static def priceplot(marketAssets:List[MarketAsset]):void{
		val mp = marketAssets(0).getMarketPrice();
		val fp = marketAssets(0).getPrice();
		for(var i:Long = 0; i < 200; i++){
			Console.OUT.println(1 + " " + (i- 100) + " " + 0 + " " + 0 + " " + mp(i) + " " + fp(i));
		}

	}
	

	//倒産した銀行の数を数える
	public static def countrupt(bankArray:ArrayList[Bank]):Long{
		var ruptnum:Long = 0;
		for(var i:Long = 0; i < N; i++){
			if(bankArray(i).status == 0){
				ruptnum++;
			}
	
		}
		return ruptnum;
		
	}

	//★大ー小ー大の数を数えるメソッド
	public static def BSBcount(bankArray:ArrayList[Bank]):Long{
		var countBSB:Long = 0;
		for(var i:Long = BigNLong; i < N; i++){
			//小銀行がつながっている大銀行の数を調べる
			var countBnum:Long = 0;
			for(var j:Long = 0; j < BigN; j++){
				if(bankArray(i).Neighbor.contains(j)){
					countBnum++;
				}
			}
		countBSB += Con(countBnum, 2);	//大銀行countBnum個とつながっている時、大小大の組み合わせはnC2
		}
		return countBSB;
	}

	//★大→小→大の個数数えるメソッド
	public static def BSBvectorcount(bankArray:ArrayList[Bank]):Long{
		var countBSB:Long = 0;	//数えるパラメータ
		for(var i:Long = 0; i < BigN; i++){
			//BSBの中から貸出の向きを指定して数える作業
			for(var j:Long = 0; j < bankArray(i).Neighbor.size(); j++){
				var checkBSB:Boolean = bankArray(bankArray(i).Neighbor(j)).NeighborOut.contains(i);		//bankArray(i).NeighborBSBからiへ貸出を行っているかのBoolean
				//Console.OUT.println(bankArray(i).Neighbor(j) + "から"+ i + "への貸出は" + checkBSB);
				if(checkBSB && bankArray(i).Neighbor(j) >= BigN){
					for(var k:Long = 0; k < BigN; k++){
						if(bankArray(k).NeighborOut.contains(bankArray(i).Neighbor(j))){
							countBSB++;
						}
					}
				}
			}
		}
		return countBSB;
	}

	//★コンビネーションnCkを計算するメソッド
	public static def Con(n:Long, k:Long):Long{
		var nume:Long = 1;
		var deno:Long = 1;
		for(var i:Long = n; i > n - k; i--){
			nume *= i;
		}
		for(var i:Long = k; i > 0; i--){
			deno *= i;
		}
		var con:Long = nume / deno;

		return con;
	}


}




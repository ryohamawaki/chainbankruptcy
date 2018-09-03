package RyoikiHamawaki;
import x10.util.*;
import x10.array.*;
import x10.lang.*;
import RyoikiHamawaki.MarketAsset;
import cassia.util.random.Gaussian;



public class Bank{
	static val m:Long = 100;					//VaRの計算に利用
	static val r_f:Double = 0.02;					//リスクフリーレート
	static val delta_t:Double = 0.004;				//Δt
	static val sigma:Double = 0.25;					//ボラティリティ
	static val M:Long = 1;						//外部資産数

	static val Threshold = 0.08;					//VaR制約の閾値
	static val Control = 0.0003;					//VaR制約の任意の係数

	static val N:Double = 100;                                          //銀行の数
	static val NLong:Long = 100;
	static val BigN:Double = 10;                                        //大銀行の数
	static val BigNLong:Long = 10;
	static val SmallN:Double = N - BigN;                                //小銀行の数

/*
	static val N:Double = 12;                                          //銀行の数
	static val NLong:Long = 12;
	static val BigN:Double = 4;                                        //大銀行の数
	static val BigNLong:Long = 4;
	static val SmallN:Double = N - BigN;                                //小銀行の数
*/
	static val stockmulti = 20;					//株価を何倍に換算するか　＝　外部資産の与える影響の制御

        //フィールド
	var ID:Long;                                               //銀行の通し番号
        var Borrowing:Double;                                      //借入金
        var Lending:Double;                                        //貸出金

	//ランダムネットワーク用
	var Neighbor:ArrayList[Long] = new ArrayList[Long]();		//makeNetの箱
  	var preNeighbor:ArrayList[Long] = new ArrayList[Long]();
	var NeighborOut:ArrayList[Long] = new ArrayList[Long]();
	var NeighborIn:ArrayList[Long] = new ArrayList[Long]();  	
	var NeighborBor:ArrayList[Long] = new ArrayList[Long]();   //つながっている銀行のIDを格納する(借入)（銀行間ネットワーク）
	var NeighborLen:ArrayList[Long] = new ArrayList[Long]();   //つながっている銀行のIDを格納する（貸出）（銀行間ネットワーク）
	//スケールフリーネットワーク用
	var SFNeighbor:ArrayList[Long] = new ArrayList[Long]();		//makeSFNetの箱

	//★大小大を数える用
	var NeighborBSB:ArrayList[Long] = new ArrayList[Long]();
	


	var status:Long;						//銀行の状態（0=倒産、1=倒産してない）

	var Omg:HashMap[Long, Double] = new HashMap[Long, Double]();
	var omega:Double;
	var count:Long = 0;	   //リンクの本数を数える
	var countOut:Long = 0;	   //リンクの本数を数える（出て行く）

	var BS:ArrayList[Double] = new ArrayList[Double]();		   //バランスシート
	var countB:Double = 0;

	var VaRjudgement:ArrayList[Boolean] = new ArrayList[Boolean]();		//VaR制約式がtrueかfalseを返す外部資産ごとに返す配列

	var ExpReturn:Double = 0;						//期待リターン

	var Gap:Double = 0;							//資金ギャップ

	var BorrowList:HashMap[Long, Double] = new HashMap[Long, Double]();
	var LendList:HashMap[Long, Double] = new HashMap[Long, Double]();

	var memoryID:ArrayList[Long] = new ArrayList[Long]();				//無限ループにならないように新規の友達のIDを保管する一時的な配列

	var ruptjudge:Boolean = false;			//銀行が倒産している条件ならtrueを、していなければfalseを返すメソッド

	var distance:Long;	//スタート銀行からの距離
	
        //コンストラクタ
	public def this(id:Long, bankstatus:Long){
		ID = id;
		status = bankstatus;
	}

	//メソッド
	//☆B1からB2にamount貸し出す
	public static def rent(B1:Bank, B2:Bank, amount:double):void{
		B1.Lending += amount;
		B2.Borrowing += amount;
	
	}

	//☆B1をネットワークに入れる
	public static def enterNet(neighbor:ArrayList[Long], B1:Bank):void{
		neighbor.add(B1.ID);

	}


	//★ネットワークのチェック
	public static def checkNet(bankArray:ArrayList[Bank]):Boolean{
		var checknet:Boolean = false;
		for(var i:Long = 0; i < N; i++){
			if(bankArray(i).NeighborOut.size() == 0){
				checknet = true;
				for(var j:Long = 0; j < N; j++){
					bankArray(j).Neighbor.clear();
					bankArray(j).NeighborOut.clear();
				}
			}
		}
		return checknet;
	}
	

	//★無向のネットワークを作るメソッド
	public static def makeNetwork(bankArray:ArrayList[Bank], rand:Random, num:Long, LinkNum:Long, LinknumB:Double, LinknumS:Double):void{
		//num=0 自作のネットワーク
		if(num == 0){
			Bank.enterNet(bankArray(0).Neighbor, bankArray(4));
			Bank.enterNet(bankArray(0).Neighbor, bankArray(5));
			Bank.enterNet(bankArray(0).Neighbor, bankArray(10));
			Bank.enterNet(bankArray(0).Neighbor, bankArray(11));
			Bank.enterNet(bankArray(1).Neighbor, bankArray(4));
			Bank.enterNet(bankArray(1).Neighbor, bankArray(5));
			Bank.enterNet(bankArray(1).Neighbor, bankArray(6));
			Bank.enterNet(bankArray(1).Neighbor, bankArray(7));
			Bank.enterNet(bankArray(2).Neighbor, bankArray(6));
			Bank.enterNet(bankArray(2).Neighbor, bankArray(7));
			Bank.enterNet(bankArray(2).Neighbor, bankArray(8));
			Bank.enterNet(bankArray(2).Neighbor, bankArray(9));
			Bank.enterNet(bankArray(3).Neighbor, bankArray(8));
			Bank.enterNet(bankArray(3).Neighbor, bankArray(9));
			Bank.enterNet(bankArray(3).Neighbor, bankArray(10));
			Bank.enterNet(bankArray(3).Neighbor, bankArray(11));
			Bank.enterNet(bankArray(4).Neighbor, bankArray(0));
			Bank.enterNet(bankArray(4).Neighbor, bankArray(1));
			Bank.enterNet(bankArray(5).Neighbor, bankArray(0));
			Bank.enterNet(bankArray(5).Neighbor, bankArray(1));
			Bank.enterNet(bankArray(6).Neighbor, bankArray(1));
			Bank.enterNet(bankArray(6).Neighbor, bankArray(2));
			Bank.enterNet(bankArray(7).Neighbor, bankArray(1));
			Bank.enterNet(bankArray(7).Neighbor, bankArray(2));
			Bank.enterNet(bankArray(8).Neighbor, bankArray(2));
			Bank.enterNet(bankArray(8).Neighbor, bankArray(3));
			Bank.enterNet(bankArray(9).Neighbor, bankArray(2));
			Bank.enterNet(bankArray(9).Neighbor, bankArray(3));
			Bank.enterNet(bankArray(10).Neighbor, bankArray(3));
			Bank.enterNet(bankArray(10).Neighbor, bankArray(0));
			Bank.enterNet(bankArray(11).Neighbor, bankArray(3));
			Bank.enterNet(bankArray(11).Neighbor, bankArray(0));

		}

		//num=1　コアプリフェラル
		if(num == 1){
			//銀行０〜９は完全ネットワーク構造
			for(var i:Long = 0; i < BigN; i++){
				for(var j:Long = 0; j < i; j++){
					Bank.enterNet(bankArray(i).Neighbor,bankArray(j));
					Bank.enterNet(bankArray(j).Neighbor,bankArray(i));
				
				}
			}

			//小銀行を９つずつ計１０クラスタに分ける。
			for(var i:Long = BigNLong; i < N; i++){
				var clustnum:Long = Long.operator_as(SmallN / BigN);
				var follow:Long = (i - BigNLong)/(clustnum);						//小銀行をクラスタ化（商で分類）
				Bank.enterNet(bankArray(follow).Neighbor,bankArray(i));
				Bank.enterNet(bankArray(i).Neighbor,bankArray(follow));
			

				//クラスタ内の４つは完全ネットワーク構造
				val rem = (i-1)%9;							//９で割ったあまりを計算
				if(rem == 8){
					for(var j:Long = i-8; j< i-4; j++){
						for(var k:Long = i-8; k < j; k++){
							Bank.enterNet(bankArray(k).Neighbor,bankArray(j));
							Bank.enterNet(bankArray(j).Neighbor,bankArray(k));
						
						}
					}
				}
			}

			/*ここまでで、大銀行は(BigN-1+clustnum)個の銀行とつながっている。
			  小銀行はBigN個のクラスタに分かれていて、その中の４つは完全ネットワーク構造でつながっているため、４個リンクを持つ銀行と１個しか持っていない銀行がある。
			  大銀行は平均３０個、小銀行は平均１０個リンクを持つようにモンテカルロ法でネットワークを作っていく。
			*/
		
			var overlap:Boolean = false;				     //被りの判定
			var pB:Double = (LinknumB - 18.0) / 90.0;				     //大銀行の残りが繋がる確率
			var pS1:Double = (44.0 * LinknumS - 251.0 - 440.0 * pB) / 7832.0;	     //小銀行のクラスタ内の銀行群が繋がる確率
			var pS2:Double = (11.0 * LinknumS + 4.0 - 110.0 * pB) / 1958.0;				     //小銀行の銀行群ではない小銀行が繋がる確率



			//大銀行の残り１２個を選ぶ
			for(var i:Long = 0; i < BigNLong; i++){
				for(var j:Long = BigNLong; j < N; j++){
					for(var k :Long = 0; k < bankArray(i).Neighbor.size(); k++){
						if(bankArray(i).Neighbor(k) == j){
							overlap = true;
						}
					}
					if(overlap){
						overlap = false;
						continue;
					}else{
						if(rand.nextDouble()<pB){
							Bank.enterNet(bankArray(i).Neighbor,bankArray(j));
							Bank.enterNet(bankArray(j).Neighbor,bankArray(i));
						
						}
					}
				}
			}
			//クラスタ内でつながっている方の小銀行の残り６個をつなぐ
			for(var i:Long = BigNLong; i < N; i++){
				for(var j:Long = BigNLong; j < N; j++){
					if(i == j){
						continue;
					}
					for(var k :Long = 0; k < bankArray(i).Neighbor.size(); k++){
						if(bankArray(i).Neighbor(k) == j){
							overlap = true;
						}
					}
					if(overlap){
						overlap = false;
						continue;
					}else{
						if((j - 1) % 9 < 4){
		 					if(rand.nextDouble()<pS1){
								Bank.enterNet(bankArray(i).Neighbor,bankArray(j));
								Bank.enterNet(bankArray(j).Neighbor,bankArray(i));

							
							}
						}else{
							if(rand.nextDouble()<pS2){
								Bank.enterNet(bankArray(i).Neighbor,bankArray(j));
								Bank.enterNet(bankArray(j).Neighbor,bankArray(i));

							
							}
						}
					}
				}
			}
		}

		//num=2 スケールフリー
		if(num == 2){
			//最初、大銀行は完全グラフでつながる		
			for(var i:Long = 0; i < BigN; i++){
				for(var j:Long = 0; j < i; j++){
					Bank.enterNet(bankArray(i).Neighbor, bankArray(j));
					Bank.enterNet(bankArray(j).Neighbor, bankArray(i));
				}
			}

			//次に、やってきた小銀行はリンクの数LinkNumに対応してリンクをはる相手を選ぶ
			for(var i:Long = BigNLong; i < N; i++){
				roulette(bankArray, i, LinkNum, rand);
			}
		}

		//num=3　ランダム
		if(num == 3){
			//大銀行は３０個の銀行とリンクを持つ,小銀行は１０個の銀行とリンクを持つ
			var sp:Double = (18.0 * LinknumS - 5.0 * LinknumB / 3.0) / 3234.0;
			var bp:Double = (LinknumS / 10.0) - (18.8 * sp);
			
			//大銀行からつなぐとき
			for(var i:Long = 0; i < BigN; i++){
				for(var j:Long = 0; j < N; j++){
					if(rand.nextDouble() <= bp){
						Bank.enterNet(bankArray(i).Neighbor, bankArray(j));
						Bank.enterNet(bankArray(j).Neighbor, bankArray(i));
					}
				}
			}

			//小銀行からつなぐとき
			for(var i:Long = BigNLong; i < N; i++){
				for(var j:Long = 0; j < N; j++){
					if(rand.nextDouble() <= sp){
						Bank.enterNet(bankArray(i).Neighbor, bankArray(j));
						Bank.enterNet(bankArray(j).Neighbor, bankArray(i));
					}
				}
			}
		}

	} 
	//★無向から有向へのメソッド
	public static def makeVector(bankArray:ArrayList[Bank], rand:Random, bsp:Double):void{
		//Neignborの向きを決める。確率1/2で決める。
		for(var i:Long = 0; i < N; i++){
				for(var j:Long = 0; j < bankArray(i).Neighbor.size(); j++){
					bankArray(i).preNeighbor(j) = bankArray(i).Neighbor(j);
				}
		}

		for(var i:Long = 0; i < N; i++){
			for(var j :Long = 0; j < bankArray(i).preNeighbor.size(); j++){
				//val randVec = new Random();
				//大銀行同士は確率1/2
				if(i < BigN && bankArray(i).preNeighbor(j) < BigN){
					if(rand.nextDouble() <= 0.5){
						bankArray(i).NeighborOut.add(bankArray(i).preNeighbor(j));
					}else{
						bankArray(bankArray(i).preNeighbor(j)).NeighborOut.add(i);
					}

					//相手の時に2重で呼び出されないようにする
					bankArray(bankArray(i).preNeighbor(j)).preNeighbor.remove(i);

					if(bankArray(i).NeighborOut.size() == 0){
						bankArray(bankArray(i).preNeighbor(j)).NeighborOut.remove(i);
						j--; 
						continue;
					}
				}//小銀行同士も確率1/2
				else if(i >= BigN && bankArray(i).preNeighbor(j) >= BigN){
					if(rand.nextDouble() <= 0.5){
						bankArray(i).NeighborOut.add(bankArray(i).preNeighbor(j));
					}else{
						bankArray(bankArray(i).preNeighbor(j)).NeighborOut.add(i);
					}

					//相手の時に2重で呼び出されないようにする
					bankArray(bankArray(i).preNeighbor(j)).preNeighbor.remove(i);

					if(bankArray(i).NeighborOut.size() == 0){
						bankArray(bankArray(i).preNeighbor(j)).NeighborOut.remove(i);
						j--;
						continue;
					}
				}//大銀行と小銀行はargsで毎回指定可
				else if(i < BigN && bankArray(i).preNeighbor(j) >= BigN){
					if(rand.nextDouble() <= bsp){
						bankArray(i).NeighborOut.add(bankArray(i).preNeighbor(j));
					}else{
						bankArray(bankArray(i).preNeighbor(j)).NeighborOut.add(i);
					}

					//相手の時に2重で呼び出されないようにする
					bankArray(bankArray(i).preNeighbor(j)).preNeighbor.remove(i);

					if(bankArray(i).NeighborOut.size() == 0){
						bankArray(bankArray(i).preNeighbor(j)).NeighborOut.remove(i);
						j--;
						continue;
					}
				}
			} 
		}

		

	}



	


	//☆重み付き行列ωを作る
	public static def makeOmg(bankArray:ArrayList[Bank], firstE:Double, rand:Random):void{

		//koutを求める（kout(i) = bankArrayOut(i).countOut）
		for(var i:Long = 0; i<NLong; i++){
			for(var j:Long = 0; j<bankArray(i).NeighborOut.size(); j++){
				bankArray(i).countOut++;
			}
		}
		//論文[Maeno]の（４）式の計算をkoutを用いて行う
		//val rand = new Random(16);
		var r:Double = rand.nextDouble();						//指数r
		val gamma_whole:Double = 0.50;							//銀行間ネットワーク全体の銀行間貸出比率0.50
		var firstL:Double = (gamma_whole/(1.0-gamma_whole))*firstE;			//すべての銀行のバランスシートの銀行間貸出の総額

		//（４）式の分母を求める
		var deno:Long = 0;			//ωの分母

		for(var i:Long = 0; i < NLong; i++){
			for(var j:Long = 0; j < bankArray(i).NeighborOut.size(); j++){
				deno += Math.pow(bankArray(i).countOut * bankArray(bankArray(i).NeighborOut(j)).countOut,r);
			}
		} 

		//（４）式の最終的な計算
		for(var i:Long = 0; i < NLong; i++){
			for(var j:Long = 0; j < bankArray(i).NeighborOut.size(); j++){
				bankArray(i).omega = Math.pow(bankArray(i).countOut * bankArray(bankArray(i).NeighborOut(j)).countOut,r) * firstL  / deno;
				bankArray(i).Omg.put(bankArray(i).NeighborOut(j), bankArray(i).omega);
			}
		}




	}

	//☆バランスシート（０資産、１外部資産、２貸出額、３自己資本、４預金、５借入額、６銀行間貸出比率、７自己資本比率、８持株数、９現金）を作る
	//★卒論でBSの作成変更（2,5 1 9 0 3 4 → 2,5 1 4 3 0 9の順に変更）
	public static def makeBS(bankArray:ArrayList[Bank], firstE:Double, marketAssets:List[MarketAsset],rand:Random):void{
		val gamma_whole:Double = 0.50;							//銀行間ネットワーク全体の銀行間貸出比率0.50
		var firstL:Double = (gamma_whole/(1.0-gamma_whole))*firstE;			//すべての銀行のバランスシートの銀行間貸出の総額
		val MP = marketAssets(0).getMarketPrice();

		//２貸出額lを求める
		var l:Double = 0;

		for(var i:Long = 0; i < NLong; i++){
			for(var j:Long = 0; j < bankArray(i).NeighborOut.size(); j++){
				l += bankArray(i).Omg.get(bankArray(i).NeighborOut(j));
			}
			bankArray(i).BS = new ArrayList[Double]();
			for(var k:Long = 0; k < 10;k++){
				bankArray(i).BS.add(0.0);
			}

			bankArray(i).BS.set(l,2); //hoge
			l =0.0;
		}

		//５借入額bを求める
		for(var i:Long = 0; i < NLong; i++){
			for(var j:Long = 0; j < bankArray(i).NeighborOut.size(); j++){
				bankArray(bankArray(i).NeighborOut(j)).countB += bankArray(i).Omg.get(bankArray(i).NeighborOut(j));
			}
		}
		for(var i:Long = 0; i < NLong; i++){
			bankArray(i).BS.set(bankArray(i).countB,5);
		}

		//１外部資産を決める
		for(var i:Long=0;i<NLong;i++){
			var e:Double = 0.0;
			var sumBL:Double = 0.0;
			var number:Double = 0;
			var numberLong:Long = 0;
			for(var j:Long = 0; j < NLong; j++){
				sumBL += Math.max(bankArray(j).BS(5) - bankArray(j).BS(2) , 0.0);
			}
			e = Math.max(bankArray(i).BS(5) - bankArray(i).BS(2), 0.0) + (firstE - sumBL) * (bankArray(i).BS(2) / firstL);
			number = e * stockmulti / MP(MP.size() - 1);		//持ち株数はstockmulti倍して考えている
			for(var j:Long = 0; j < number; j++){
				numberLong++;					//持ち株数を整数の離散値で表す
			}
			e = 0;
			e = MP(MP.size() - 1) * numberLong / stockmulti;			//持株数から外部資産を算出する
			bankArray(i).BS.set(e,1);
			bankArray(i).BS.set(numberLong,8);
		}

		//４預金dを決める
		for(var i:Long=0;i<NLong;i++){
			var d:Double = bankArray(i).NeighborOut.size() * (40 + 20 * rand.nextDouble());	//預金d=(貸出リンク数)*rand(20~40)で決定
			bankArray(i).BS.set(d,4);
		}

		//３自己資本c、自己資本比率CARを決める
		//val randC = new Random(16);
		for(var i:Long=0;i<NLong;i++){
			var car:Double = 0.10 + 0.20 * rand.nextDouble();	//自己資本比率の決定（ランダム10%~30%）
			var c:Double = (bankArray(i).BS(4) + bankArray(i).BS(5)) * (car/(1-car));
			bankArray(i).BS.set(c,3);
			bankArray(i).BS.set(car,7);
		}

		//０資産aと銀行間貸出比率γを決める
		for(var i:Long=0;i<NLong;i++){
			var a:Double=0.0;
			a = Math.max(bankArray(i).BS(9) + bankArray(i).BS(1) + bankArray(i).BS(2), 
                                                  bankArray(i).BS(3) + bankArray(i).BS(4) + bankArray(i).BS(5));	//資産a=max(現金５０+外部資産e+銀行間貸出l, 自己資本c+預金d+銀行間借入b)
			bankArray(i).BS.set(a,0);
			var gamma:Double = 0.0;
			gamma = bankArray(i).BS(2)/bankArray(i).BS(0);	//銀行間貸出比率γ=銀行間貸出l/資産a
			bankArray(i).BS.set(gamma,6);
		}

		

		//９現金を決める
		for(var i:Long = 0; i < NLong; i++){
			var m:Double = 0.0;
			m = bankArray(i).BS(3) + bankArray(i).BS(4) + bankArray(i).BS(5) - (bankArray(i).BS(1) + bankArray(i).BS(2));	//現金　＝（総資産）-（（市場性資産）+（貸出））で算出
		
			bankArray(i).BS.set(m, 9);
		}

		

		

		

		
	}

	//☆貸し借り表を作る
	public static def BandLList(bankArray:ArrayList[Bank], firstE:Double):void{
		for(var i:Long = 0; i < N; i++){
			for(var j:Long = 0; j < bankArray(i).NeighborOut.size(); j++){
				val get = bankArray(i).Omg.get(bankArray(i).NeighborOut(j));

				bankArray(i).LendList.put(bankArray(i).NeighborOut(j),get);
				bankArray(bankArray(i).NeighborOut(j)).BorrowList.put(i, get);
			}
		}
		
	}

	
	//外部資産への投資に使う期待リターンを求める
	public static def makeExpReturn(bankArray:ArrayList[Bank], marketAssets:List[MarketAsset], rand:Random):void{
		//ファンダメンタル項について
		var tauF:Double = 0.01;				//平均回帰速度
		val Fp = marketAssets(0).getPrice();		//理論価格の取得
		val Mp = marketAssets(0).getMarketPrice();	//市場価格の取得
		val fundamentalLogReturn = tauF * Math.log(Fp(Fp.size()-1) / Mp(Mp.size()-1));

		//テクニカル項について
		var tauC:Long = 50;				//参照する過去のデータ数
		var chartMeanLogReturn:Double = 0;
		for(var i:Long = (Mp.size() - 1); i > (Mp.size() - tauC - 1); i--){
			chartMeanLogReturn += Math.log(Mp(i)/Mp(i-1));
		}
		chartMeanLogReturn = chartMeanLogReturn / tauC;

		//ノイズ項について
		//val randnoise = new Random();
		val gaussian = new Gaussian(rand);
		val noiseScale:Double = 0.001;

		val noiseLogReturn = 0.0 + noiseScale * gaussian.nextGaussian();



		//各銀行について重み付けを乱数で作る
		for(var i:Long = 0; i < NLong; i++){
			val randExp = new Random();
			val weightF = 5 * rand.nextDouble();
			val weightC = rand.nextDouble();
			val weightN = rand.nextDouble();
			val norm = weightF + weightC + weightN;

			bankArray(i).ExpReturn = (weightF * fundamentalLogReturn + weightC * chartMeanLogReturn + weightN * noiseLogReturn) / norm;
			

		}




	}
	//倒産した銀行の処理


	//★ルーレット選択
	public static def roulette(bankArray:ArrayList[Bank], ID:Long, pickupNum:Long, rand:Random):void{
		for(var i:Long = 1; i <= pickupNum; i++){
			var LinkSum:Double = 0;
			//リンクの本数の合計を数える
			for(var j:Long = 0; j < N; j++){
				LinkSum += bankArray(j).Neighbor.size();
			}
			val RS = rand.nextDouble();
			var rs:Double = RS;
			var jcount:Long = -1;

			
			for(var j:Long = 0; rs > 0; j++){
				//Console.OUT.println("LinkSumは" + LinkSum);
				var minusRS:Double = bankArray(j).Neighbor.size() / LinkSum;
				//Console.OUT.println("minusRSは" + minusRS);
				rs -= minusRS;
				//Console.OUT.print(rs +  " ");
				jcount++;
			}

			//Console.OUT.println("銀行" + ID + "の" + (i+2) + "番目の繋がる銀行は" + jcount);
			
			//二重に取ってこないための確認
			var checkRS:Boolean = true;
			if(ID == jcount){
				checkRS = false;
			}
			for(var k:Long = 0; k < bankArray(ID).Neighbor.size(); k++){
				if(bankArray(ID).Neighbor(k) == jcount){
					checkRS = false;
				}
			}

			//二重に取っていなければNeighborに追加,被っていればもう一回やり直し
			if(checkRS){
				Bank.enterNet(bankArray(ID).Neighbor, bankArray(jcount));
				Bank.enterNet(bankArray(jcount).Neighbor, bankArray(ID));
			}
			if(!checkRS){
				i--;
			}
		}
	}

	//★銀行Xからの距離を計算するメソッド
	public static def caldistance(bankArray:ArrayList[Bank], ID:Long):void{
		//重複しないための箱を作っておく
		var Bankbox:ArrayList[Long] = new ArrayList[Long]();
		for(var i:Long = 0; i < N; i++){
			Bankbox.add(i);
		}
		//X=IDの時距離はゼロ
		bankArray(ID).distance = 0;
		Bankbox.remove(ID);

		//bankArray(ID).NeighborInに入っている時距離は１
		for(var i:Long = 0; i < bankArray(ID).NeighborIn.size(); i++){
			bankArray(bankArray(ID).NeighborIn(i)).distance = 1;
			Bankbox.remove(bankArray(ID).NeighborIn(i));
		}

		//以下、Bankboxが空になるまで操作を続ける
		for(var i:Long = 1; Bankbox.size() > 0; i++){
			for(var j:Long = 0; j < N; j++){
				for(var k:Long = 0; k < bankArray(j).NeighborIn.size(); k++){
					if(bankArray(j).distance == i){
						if(Bankbox.contains(bankArray(j).NeighborIn(k))){
							bankArray(bankArray(j).NeighborIn(k)).distance = i + 1;
							Bankbox.remove(bankArray(j).NeighborIn(k));
						}
					}
				}
			}
		}

		
	}

}


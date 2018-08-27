package RyoikiHamawaki;
import x10.util.*;
import x10.array.*;
import x10.lang.*;

public class MarketAsset {
	//フィールド
	var firstPrice:Double;					//価格の初期値
	var price:ArrayList[Double];				//過去m日間における理論価格
	var MarketPrice:ArrayList[Double];			//過去m日間における市場価格
	var r_avg:Double;					//過去m日間におけるリターンの平均
	var sigma_m:Double;					//過去m日間におけるリターンの標準偏差
	var u:Double;						//期待効用
	//メソッド
	//①setterメソッド
	public def setFirstPrice(newFirstPrice:Double){
		this.firstPrice = newFirstPrice;
	}
	public def setPrice(newPrice:ArrayList[Double]){
		this.price = newPrice;
	}
	public def setMarketPrice(newPrice:ArrayList[Double]){
		this.MarketPrice = newPrice;
	}
	public def setR_avg(newR_avg:Double){
		this.r_avg = newR_avg;
	}
	public def setSigma_m(newSigma_m:Double){
		this.sigma_m = newSigma_m;
	}
	//②getterメソッド
	public def getFirstPrice():Double{
		return this.firstPrice;
	}
	public def getPrice():ArrayList[Double]{
		return this.price;
	}
	public def getMarketPrice():ArrayList[Double]{
		return this.MarketPrice;
	}
	public def getR_avg():Double{
		return this.r_avg;
	}
	public def getSigma_m():Double{
		return this.sigma_m;
	}
}

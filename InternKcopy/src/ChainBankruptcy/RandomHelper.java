package ChainBankruptcy;
import java.util.*;


public class RandomHelper {
    public Random random;
    public Gaussian g;

    public RandomHelper(Random random) {
        this.random = random;
        this.g = new Gaussian(random);
    }


    public Double nextNormal(double mu, double sigma){
        return mu + g.nextGaussian() * sigma;
    }

}

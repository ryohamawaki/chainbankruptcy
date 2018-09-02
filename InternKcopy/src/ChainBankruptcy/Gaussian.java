package ChainBankruptcy;
import java.util.*;

public class Gaussian {
    boolean state;
    double g;
    Random random;

    public Gaussian(Random random) {
        this.state = false;
        this.g = -1;
        this.random = random;
    }


    public double nextGaussian(){
        if (state) {
            state = false;
            return this.g;
        } else {
            state = true;
            double v1;
            double v2;
            double s;
            do {
                v1 = 2.0 * random.nextDouble() - 1.0;
                v2 = 2.0 * random.nextDouble() - 1.0;
                s = v1 * v1 + v2 * v2;
            } while (s >= 1.0);

            double norm = Math.sqrt(-2.0 * Math.log(s) / s);
            this.g = v2 * norm;
            return v1 * norm;
        }
    }

    public double nextGaussian(double mu, double sigma) {
        return mu + sigma * this.nextGaussian();
    }

}

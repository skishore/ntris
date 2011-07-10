package ntris_src;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class PRG {
    private List<Integer> primes;
    private boolean safe = false;
    private long x;
    private int p, q, n;
    private Random rand;

    public PRG(long seed) {
        this(seed, false); 
    }

    public PRG(long seed, boolean safety) {
        primes = new ArrayList<Integer>();
        List<Integer> allPrimes = new ArrayList<Integer>();
        int smallPrimes = 1;
        boolean isPrime;
        double root; 

        rand = new Random();

        allPrimes.add(2);
        for (int i = 3; i < 256; i+=2) {
            isPrime = true;
            root = Math.sqrt(i);
            for (int j = 0; j < allPrimes.size(); j++) {
                if (allPrimes.get(j) > root) 
                    break;
                else if (i%allPrimes.get(j) == 0) {
                    isPrime = false;
                    break;
                }
            }
            if (isPrime) {
                allPrimes.add(i);
                if (i%4 == 3) {
                    primes.add(i);
                    if (i < 128) smallPrimes++;
                }
            }
        }

        for (int j = 0; j < smallPrimes; j++) {
            primes.remove(0);
        }
        
        seedPRG(seed, safety);
    }

    public void seedPRG(long seed, boolean safety) {
        safe = safety;
        
        if (safe) {
            rand.setSeed(seed);
            return;
        }

        int numPrimes = primes.size();
        int pIndex = Math.abs((int)seed%numPrimes);
        int qIndex = Math.abs(((int)seed/numPrimes)%numPrimes);
        //if (qIndex >= pIndex) qIndex++;

        p = primes.get(pIndex);
        q = primes.get(qIndex);
        n = p*q;
        x = (seed/(numPrimes*numPrimes))%n;

        //if (x%p == 0) x += q;
        //if (x%q == 0) x += p;
    }

    // returns a pseudorandom 32-bit integer
    long generate() {
        int curBit = 1;
        long result = 0;

        if (safe) return rand.nextLong();

        for (int i = 0; i < 37; i++) {
            x = (x*x)%n;
            if (i < 32) {
                result += curBit*(x%2);
                curBit = 2*curBit;
            }
        }

        return result;
    }
}

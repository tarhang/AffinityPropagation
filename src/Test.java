/*
This file contains the main() to test the code. After calling the main, expect to get
several .txt files each containing a single cluster from the data set. Hence, the number
of .txt files indicates the number of clusters.
 */
public class Test
{
    public static void main(String[] args)
    {
        String filename = "copy.csv";
        double slider = 0.05;
        AffinityPropagation one = new AffinityPropagation(filename, slider);

        one.apCluster();
        one.setNumClusters();
        one.writeFile_matlab();
    }
}

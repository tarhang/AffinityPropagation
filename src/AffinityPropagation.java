import java.io.*;
import java.util.*;


public class AffinityPropagation
{
    // input file information
    private String filename;
    private String sourceDeviceModel;
    private String building;
    private int floor;

    // data point information
    private int numReferencePoints, numAccessPoints;
    private ReferencePoint[] points;

    // affinity propagation input
    private double[][] psi;
    private double[][] similarities;
    private double gama = 0.36; // for now so the code works as it should!
    private double dampingFactor = 0.5;
    private double slider;

    // affinity propagation parameters
    private double[][] responsibilities;
    private double[][] availablities;

    // affinity propagation output
    private double[][] combined;
    private int numClusters;

    // extra - house keeping
    private static int fileNumber = 0;
    private static int filenumber_matlab = 0;


    /**
     * Constructor of the AffinityPropagation class for the case when no 'slider' parameter
     * is provided (slider=0.5 assumed).
     * @param filename file address to the data set
     */
    public AffinityPropagation(String filename)
    {
        this.filename = filename;
        this.numClusters = 0;
        this.slider = 0.5;

        // setting other data structures
        setPoints();
        affinityPropagation_instantiation();
        setSimilarities();
    }


    /**
     * Constructor of the AffinityPropagation class for the case when a slider value
     * is provided.
     * @param filename file address to the data set
     * @param slider double value between 0-1
     */
    public AffinityPropagation(String filename, double slider)
    {
        this.filename = filename;
        this.numClusters = 0;
        this.slider = slider;

        // setting other data structures
        setPoints();
        affinityPropagation_instantiation();
        setSimilarities();
    }


    public int getNumClusters()
    {
        return numClusters;
    }


    public int getNumReferencePoints()
    {
        return numReferencePoints;
    }


    public int getNumAccessPoints()
    {
        return numAccessPoints;
    }


    /**
     * prints the matrix to the console
     * @param matrix a 2D matrix of doubles (similarity, responsibility, availability, combined)
     * @param name name of the matrix (similarity, responsibility, availability, combined)
     */
    public void printMatrix(double[][] matrix, String name)
    {
        System.out.println(name + ":");

        for(int i = 0; i < matrix.length; i += 1)
        {
            for(int j = 0; j < matrix[i].length; j += 1)
            {
                System.out.print(matrix[i][j] + "	");
            }

            System.out.println();
        }

        System.out.println();
    }


    /**
     * prints the cluster passed to the function
     * @param cluster an array list of reference points in one cluster
     */
    public void printCluster(ArrayList<ReferencePoint> cluster)
    {
        System.out.print("{");

        for(ReferencePoint point : cluster)
        {
            System.out.print(point);
        }

        System.out.println("}");
    }


    /**
     * sorts an array of double in ascending order in place
     * @param array a 1D array of doubles
     */
    private void selectionSort(double[] array)
    {
        double temp;

        for(int i = 0; i < array.length; i += 1)
        {
            for(int j = i; j < array.length; j += 1)
            {
                if(array[j] < array[i])
                {
                    temp = array[i];
                    array[i] = array[j];
                    array[j] = temp;
                }
            }
        }
    }


    /**
     * opens the .csv file associated with this object
     * @return returns a BufferedReader pointer pointing to the file
     */
    private BufferedReader openFile()
    {
        try
        {
            FileReader fp = new FileReader(filename);
            BufferedReader toRead = new BufferedReader(fp);
            return toRead;
        }

        catch(IOException e)
        {
            return null;
        }
    }


    /**
     * reads the .csv file associated with this object and stores its information
     */
    public void setPoints()
    {
        BufferedReader toRead = openFile();
        String line;
        String[] parts;
        ReferencePoint newPoint = null;

        double x, y;
        char orientation;
        int AP_added = 0, reference_added = 0;
        boolean orientation_active = false, RSS_active = false;

        if(toRead != null)
        {
            try
            {
                line = toRead.readLine();

                while(line != null)
                {
                    parts = line.split(",");

                    if(parts.length == 1)
                    {
                        if(parts[0].equals("end_of_point"))
                        {
                            orientation_active = false;
                            RSS_active = false;
                        }

                        if(RSS_active == true)
                        {
                            newPoint.RSS.add(Double.parseDouble(parts[0]));
                        }
                    } // end of IF

                    else if(parts.length == 2)
                    {
                        if(parts[0].equals("source_device_model"))
                        {
                            sourceDeviceModel = parts[1];
                        }

                        else if(parts[0].equals("building"))
                        {
                            building = parts[1];
                        }

                        else if(parts[0].equals("floor"))
                        {
                            floor = Integer.parseInt(parts[1]);
                        }

                        else if(parts[0].equals("number_of_points"))
                        {
                            numReferencePoints = Integer.parseInt(parts[1]);
                            points = new ReferencePoint[numReferencePoints];
                        }

                        else if(parts[0].equals("MACListLength"))
                        {
                            numAccessPoints = Integer.parseInt(parts[1]);
                        }
                    } // end of ELSE IF

                    else if(parts.length == 3)
                    {
                        if(parts[0].equals("Coordinate"))
                        {
                            x = Double.parseDouble(parts[1]);
                            y = Double.parseDouble(parts[2]);
                            newPoint = new ReferencePoint(x, y);
                            points[reference_added] = newPoint;
                            reference_added += 1;
                        }

                        else if(parts[0].equals("Labeled Point"))
                        {
                            orientation_active = true;
                        }

                        else if(orientation_active == true)
                        {
                            orientation = parts[0].charAt(0);
                            newPoint.orientation = orientation;
                            RSS_active = true;
                        }

                        else if(parts[0].equals("psi_matrix"))
                        {
                            int row = Integer.parseInt(parts[1]);
                            int column = Integer.parseInt(parts[2]);
                            psi = new double[row][column];
                        }
                    } // end of ELSE IF

                    else if(parts.length == numReferencePoints)
                    {
                        for(int i = 0; i < parts.length; i += 1)
                        {
                            psi[AP_added][i] = Double.parseDouble(parts[i]);
                        }

                        AP_added += 1;
                    } // end of ELSE IF

                    line = toRead.readLine();
                }
            } // end of TRY

            catch (IOException e)
            {
                e.printStackTrace();
            }
        } // end of IF
    }


    /**
     * just a couple of instantiations to do affinity propagation
     */
    public void affinityPropagation_instantiation()
    {
        availablities = new double[numReferencePoints][numReferencePoints];
        responsibilities = new double[numReferencePoints][numReferencePoints];
        combined = new double[numReferencePoints][numReferencePoints];
        similarities = new double[numReferencePoints][numReferencePoints];
    }


    /**
     * calculates how similar reference points a and b are based on their RSS readings
     * @param a a reference point
     * @param b a reference point
     * @return returns a double indicating how similar a and b are
     */
    private double calculateSimilarity(ReferencePoint a, ReferencePoint b)
    {
        double similarity = 0.0;

        for(int i = 0; i < a.RSS.size(); i += 1)
        {
            similarity -= Math.pow(a.RSS.get(i) - b.RSS.get(i), 2);
        }

        return similarity;
    }


    /**
     * calculates the common self similarity, or preference, given a similarity matrix as the median of other similarities
     * @param similarity a 2D array of doubles
     * @param gama a double to avoid numerical oscillations
     * @return returns the self similarity, or preference, of the input
     */
    private double calculatePreference(double[][] similarity, double gama)
    {
        int size = (int) (Math.pow(similarity.length, 2) - similarity.length);
        double[] allSimilarities = new double[size];
        int index, added = 0;
        double preference;

        // creating an array consisting of all off main diagonal elements in the similarity
        // matrix (all similarities, not preferences)
        for(int i = 0; i < similarity.length; i += 1)
        {
            for(int j = 0; j < similarity[i].length; j += 1)
            {
                if(i != j)
                {
                    allSimilarities[added] = similarity[i][j];
                    added += 1;
                }
            }
        }

        // sorting similarities from smallest to largest
        selectionSort(allSimilarities);

        // finding the "shifted median"
        index = (int) (allSimilarities.length * slider);
        preference = allSimilarities[index] * gama;

        return preference;
    }


    /**
     * sets the similarities matrix based on pairwise similarities
     */
    public void setSimilarities()
    {
        ReferencePoint a, b;
        double similarity, preference;

        for(int i = 0; i < points.length; i += 1)
        {
            for(int j = 0; j < points.length; j += 1)
            {
                if(i != j)
                {
                    a = points[i];
                    b = points[j];
                    similarity = calculateSimilarity(a, b);
                    similarities[i][j] = similarity;
                }
            }
        }

        // inputting preferences into the similarity matrix
        preference = calculatePreference(similarities, gama);

        for(int i = 0; i < points.length; i += 1)
        {
            similarities[i][i] = preference;
        }
    }


    /**
     * calculates and sets the responsibility message sent from reference point with indexA to reference point with indexB
     * @param indexA index of the first reference point
     * @param indexB index of the second reference point
     */
    private void setResponsibility(int indexA , int indexB)
    {
        double responsibility, prev = responsibilities[indexA][indexB];
        Double curr, max = -1.0 * Double.MAX_VALUE;
        int i;

        for(i = 0; i < points.length; i += 1)
        {
            if(i != indexB)
            {
                curr = availablities[indexA][i] + similarities[indexA][i];

                if(curr > max)
                {
                    max = curr;
                }
            }
        }

        responsibility = similarities[indexA][indexB] - max;
        responsibility = dampingFactor * prev + (1 - dampingFactor) * responsibility;
        responsibilities[indexA][indexB] = responsibility;
    }


    /**
     * calculates the sets the availability message sent from reference point with indexB to reference point with indexA
     * @param indexA first refernece point
     * @param indexB second reference point
     */
    private void setAvailability(int indexA, int indexB)
    {
        double availability, curr, prev = availablities[indexA][indexB], sum = 0.0;
        int i;

        // calculating the expression: sum(max{0,r(i',k)}) where i' is not equal to i and k
        for(i = 0; i < responsibilities.length; i += 1)
        {
            if(i != indexA && i != indexB)
            {
                curr = responsibilities[i][indexB];

                if(curr < 0)
                {
                    curr = 0.0;
                }

                sum += curr;
            }
        }

        // this will be the final availability if reference points a and b are the same
        availability = sum;

        if(indexA != indexB)
        {
            availability += responsibilities[indexB][indexB];

            if(availability > 0)
            {
                availability = 0.0;
            }
        }

        // to avoid numerical oscillations
        availability = dampingFactor * prev + (1 - dampingFactor) * availability;
        availablities[indexA][indexB] = availability;
    }


    /**
     * updates all pairwise responsibility messages between all reference points
     */
    public void updateResponsibilities()
    {
        for(int i = 0; i < points.length; i += 1)
        {
            for(int j = 0; j < points.length; j += 1)
            {
                setResponsibility(i, j);
            }
        }
    }


    /**
     * updates all pairwise availability messages between all reference points
     */
    public void updateAvailabilities()
    {
        for(int i = 0; i < points.length; i += 1)
        {
            for(int j = 0; j < points.length; j += 1)
            {
                setAvailability(i, j);
            }
        }
    }


    /**
     * updates the combined matrix as the sum of availability and responsibility
     */
    public void updateCombined()
    {
        double val;

        for(int i = 0; i < combined.length; i += 1)
        {
            for(int j = 0; j < combined[i].length; j += 1)
            {
                val = availablities[i][j] + responsibilities[i][j];
                combined[i][j] = val;
            }
        }
    }


    /**
     * finds out and sets the number of cluster in the given orientation
     */
    public void setNumClusters()
    {
        ArrayList<ArrayList<ReferencePoint>> clusters = new ArrayList<ArrayList<ReferencePoint>>();
        ArrayList<ReferencePoint> cluster;

        for(ReferencePoint point : points)
        {
            cluster = getCluster(point);

            // adding the new cluster to the group of cluster + writing the cluster to the file
            if(clusters.contains(cluster) == false)
            {
                clusters.add(cluster);
            }
        }

        numClusters = clusters.size();
    }


    /**
     * finds the reference point that best represents the current reference point
     * @param point_index index of the reference point in the oriented points array list that we want to find the examplar for
     */
    public void findExamplar_point(int point_index)
    {
        Double max = -1.0 * Double.MAX_VALUE;
        double curr;
        ReferencePoint point = points[point_index];
        ReferencePoint prev_examplar = point.examplar;

        // looping though all reference points
        for(int k = 0; k < points.length; k += 1)
        {
            curr = combined[point_index][k];

            if(curr > max)
            {
                max = curr;
                point.examplar = points[k];
            }
        }

        // checking whether or not the examplar has changed
        if(point.examplar.equals(prev_examplar) == true)
        {
            point.examplar_changed = false;
        }

        // updating who is examplar who is not
        point.examplar.cluster_head = true;

        if(prev_examplar != null && point.examplar.equals(prev_examplar) == false)
        {
            prev_examplar.cluster_head = false;
        }
    }


    /**
     * finds the examplars of all reference points in the given orientation
     */
    public void findExamplars()
    {
        for(int i = 0; i < points.length; i += 1)
        {
            findExamplar_point(i);
        }
    }


    /**
     * determines whether or not the clustering process should be terminated
     * @return returns true if local examplars have not changed from the previous iteration of the algorithm; false otherwise
     */
    public boolean terminate()
    {
        boolean terminate = true;

        for(int i = 0; i < points.length; i += 1)
        {
            if(points[i].examplar_changed == true)
            {
                terminate = false;
                break;
            }
        }

        return terminate;
    }


    /**
     * does the actual clustering
     */
    public void apCluster()
    {
        int unchanged = 0;

        while(unchanged < 10)
        {
            updateResponsibilities();
            updateAvailabilities();
            updateCombined();
            findExamplars();

            if(terminate() == true)
            {
                unchanged += 1;
            }

            else
            {
                unchanged = 0;
            }
        }
    }


    /**
     * finds the cluster to which the passed reference point belongs to
     * @param point a reference point
     * @return returns an array list with the cluster head at its first place followed by other members of the cluster
     */
    public ArrayList<ReferencePoint> getCluster(ReferencePoint point)
    {
        ArrayList<ReferencePoint> cluster = new ArrayList<ReferencePoint>();

        ReferencePoint cluster_head = point.examplar;
        cluster.add(cluster_head);

        for(ReferencePoint each_point : points)
        {
            if(each_point.examplar.equals(cluster_head) == true)
            {
                cluster.add(each_point);
            }
        }

        return cluster;
    }


    /**
     * creates a .csv file filename
     * @return returns a string as the file name
     */
    public String createFileName()
    {
        String toReturn = "cluster" + fileNumber + ".csv";
        fileNumber += 1;

        return toReturn;
    }


    /**
     * creates a new .csv file
     * @param filename name of the file
     * @return returns a PrintWriter pointer to the newly created file
     */
    public PrintWriter createFile(String filename)
    {
        try
        {
            FileWriter fp = new FileWriter(filename);
            PrintWriter pw = new PrintWriter(fp);
            return pw;
        }

        catch(IOException e)
        {
            return null;
        }
    }


    /**
     * writes one cluster to the given file
     * @param pw a PrintWriter pointer to the .csv file
     * @param cluster an array list indicating one cluster
     */
    public void writeCluster(PrintWriter pw, ArrayList<ReferencePoint> cluster)
    {
        for(ReferencePoint point : cluster)
        {
            pw.append("begin_new_point" + "\n");
            pw.append("Coordinate," + point.x + "," + point.y + "\n");
            pw.append(point.orientation + "\n");

            for(int i = 0; i < point.RSS.size(); i += 1)
            {
                pw.append(point.RSS.get(i) + "," + "\n");
            }

            pw.append("end_of_point" + "\n");
        }
    }


    /**
     * creates and writes as many as needed files in one orientation
     */
    public void writeFile()
    {
        ArrayList<ArrayList<ReferencePoint>> clusters = new ArrayList<ArrayList<ReferencePoint>>();
        ArrayList<ReferencePoint> cluster;
        String filename;
        PrintWriter pw;

        for(ReferencePoint point : points)
        {
            cluster = getCluster(point);

            // adding the new cluster to the group of cluster + writing the cluster to the file
            if(clusters.contains(cluster) == false)
            {
                clusters.add(cluster);
                filename = createFileName();
                pw = createFile(filename);

                if(pw != null)
                {
                    pw.append("source_device_model," + sourceDeviceModel + "\n");
                    pw.append("building," + building + "\n");
                    pw.append("floor," + floor + "\n");
                    pw.append("number_of_points," + cluster.size() + "\n");

                    writeCluster(pw, cluster);
                    pw.close();
                }
            }
        }
    }


    public String createFilename_matlab()
    {
        String toReturn = "cluster_matlab" + filenumber_matlab + ".txt";
        filenumber_matlab += 1;

        return toReturn;
    }


    public void writeCluster_matlab(PrintWriter pw, ArrayList<ReferencePoint> cluster)
    {
        for(ReferencePoint point : cluster)
        {
            pw.append(point.x + " " + point.y + "\n");
        }
    }


    public void writeFile_matlab()
    {
        ArrayList<ArrayList<ReferencePoint>> clusters = new ArrayList<ArrayList<ReferencePoint>>();
        ArrayList<ReferencePoint> cluster;
        String filename;
        PrintWriter pw;

        for(ReferencePoint point : points)
        {
            cluster = getCluster(point);

            // adding the new cluster to the group of cluster + writing the cluster to the file
            if(clusters.contains(cluster) == false)
            {
                clusters.add(cluster);
                filename = createFilename_matlab();
                pw = createFile(filename);

                if(pw != null)
                {
                    writeCluster_matlab(pw, cluster);
                    pw.close();
                }
            }
        }
    }
}

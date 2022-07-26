package com.example.agricultureoptimizerbackend.utils;

import com.opencsv.CSVReader;
import org.gnu.glpk.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class DataFileReader {
    private static FileReader fileReader;

    public static void test() {

        glp_prob lp;
        SWIGTYPE_p_int ia;
        SWIGTYPE_p_int ja;
        SWIGTYPE_p_double d;

        lp = GLPK.glp_create_prob();
        ia = GLPK.new_intArray(10);
        ja = GLPK.new_intArray(10);
        d = GLPK.new_doubleArray(10);
        GLPK.glp_set_prob_name(lp, "sample");
        GLPK.glp_set_obj_dir(lp, GLPKConstants.GLP_MAX);
        String[] titleRow = {};

        double[] costs = readCosts();
        double[] prices = readPrices();
        double[][] rotatios = readRotation();


        double[] profit = new double[costs.length];

        for (int i = 0; i < costs.length; i++) {
            profit[i] = prices[i] - costs[i];
        }

        printArray(costs);
        printArray(prices);
        printArray(profit);
        printMatrix(rotatios);

        d = GLPK.new_doubleArray(costs.length + 1);
        ia = GLPK.new_intArray(costs.length + 1);
        ja = GLPK.new_intArray(costs.length + 1);

        for (int i = 1; i <= costs.length; i++) {
            GLPK.doubleArray_setitem(d, i, costs[i - 1]);
            GLPK.intArray_setitem(ia, i, 1);
            GLPK.intArray_setitem(ja, i, i);
        }

        double maxInvest = 2000.0;
        GLPK.glp_add_rows(lp, 1);
        GLPK.glp_set_row_name(lp, 1, "c");
        GLPK.glp_set_row_bnds(lp, 1, GLPKConstants.GLP_UP, 0.0, maxInvest);

        GLPK.glp_add_cols(lp, prices.length);

        for (int i = 0; i < profit.length; i++) {
            GLPK.glp_set_col_name(lp, i + 1, "x" + (i + 1));
            GLPK.glp_set_col_bnds(lp, i + 1, GLPKConstants.GLP_LO, 0.0, 0.0);
            GLPK.glp_set_obj_coef(lp, i + 1, profit[i]);
        }

        double[] solution = new double[prices.length];

        GLPK.glp_load_matrix(lp, prices.length, ia, ja, d);
        GLPK.glp_simplex(lp, null);
        double z = GLPK.glp_get_obj_val(lp);

        for (int i = 0; i < prices.length; i++) {
            solution[i] = GLPK.glp_get_col_prim(lp, i + 1);
        }


        System.out.println("z = " + z + "\n");
        printArray(solution);

        GLPK.glp_delete_prob(lp);
    }


    public static double[] readCosts() {
        String[] titleRow = {};

        double[] costs = null;

        try {
            fileReader = new FileReader("data/custos.csv");
            CSVReader csvReader = new CSVReader(fileReader);
            String[] nextRecord;
            titleRow = csvReader.readNext();

            costs = new double[titleRow.length - 1];

            while ((nextRecord = csvReader.readNext()) != null) {
                for (int i = 0; i < nextRecord.length; i++) {
                    String cell = nextRecord[i];
                    if (i >= 1) {
                        costs[i - 1] += Double.parseDouble(cell);
                    }
                    System.out.print(cell + "\t");
                }
                System.out.println();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return costs;
    }

    public static double[] readPrices() {

        double[] prices = null;

        try {
            fileReader = new FileReader("data/precos.csv");
            CSVReader csvReader = new CSVReader(fileReader);
            String[] nextRecord;
            String[] titleRow = csvReader.readNext();

            prices = new double[titleRow.length - 1];

            while ((nextRecord = csvReader.readNext()) != null) {
                for (int i = 0; i < nextRecord.length; i++) {
                    String cell = nextRecord[i];
                    if (i >= 1) {
                        prices[i - 1] += Double.parseDouble(cell);
                    }
                }
                System.out.println();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return prices;
    }

    public static double[] readSpaces() {

        double[] spaces = null;

        try {
            fileReader = new FileReader("data/espaco.csv");
            CSVReader csvReader = new CSVReader(fileReader);
            String[] nextRecord;
            String[] titleRow = csvReader.readNext();

            spaces = new double[titleRow.length - 1];

            while ((nextRecord = csvReader.readNext()) != null) {
                for (int i = 0; i < nextRecord.length; i++) {
                    String cell = nextRecord[i];
                    if (i >= 1) {
                        spaces[i - 1] += Double.parseDouble(cell);
                    }
                }
                System.out.println();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return spaces;
    }

    public static double[][] readRotation() {

        double[][] rotation = null;

        try {
            fileReader = new FileReader("data/rotacao.csv");
            CSVReader csvReader = new CSVReader(fileReader);
            String[] nextRecord;
            String[] titleRow = csvReader.readNext();

            rotation = new double[titleRow.length - 1][titleRow.length - 1];

            for (int i = 0; (nextRecord = csvReader.readNext()) != null; i++) {
                for (int j = 0; j < titleRow.length - 1; j++) {
                    String cell = nextRecord[j];
                    if (j >= 1) {
                        rotation[i][j - 1] = Double.parseDouble(cell);
                    }
                }
                System.out.println();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rotation;
    }


    public static void printArray(double[] array) {
        for (double d : array) {
            System.out.print(d + ",");
        }
        System.out.println();
    }

    public static void printMatrix(double[][] matrix) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                System.out.print(matrix[i][j] + ",");
            }
            System.out.println();
        }
    }
}

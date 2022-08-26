package com.example.agricultureoptimizerbackend.controllers;

import com.example.agricultureoptimizerbackend.dto.CropDTO;
import com.example.agricultureoptimizerbackend.dto.InputDataDTO;
import com.example.agricultureoptimizerbackend.dto.SolutionCropDTO;
import com.example.agricultureoptimizerbackend.dto.SolutionDTO;
import com.example.agricultureoptimizerbackend.model.*;
import com.example.agricultureoptimizerbackend.services.CropService;
import com.example.agricultureoptimizerbackend.services.InputDataService;
import com.example.agricultureoptimizerbackend.services.SolutionCropService;
import com.example.agricultureoptimizerbackend.services.SolutionService;
import com.example.agricultureoptimizerbackend.utils.DataFileReader;
import org.gnu.glpk.*;
import org.hibernate.Criteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.Entity;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/main")
public class MainController {

    @Autowired
    CropService cropService;
    @Autowired
    SolutionService solutionService;
    @Autowired
    SolutionCropService solutionCropService;
    @Autowired
    InputDataService inputDataService;

    @GetMapping(value = "/test")
    public ResponseEntity<String> test(HttpServletResponse response) {
        System.out.println("Test");


        Crop crop = new Crop("alface", 5.0, 2.0, 1.0, 1.0);
        cropService.save(crop);
        return ResponseEntity.ok("Test");
    }

    @PostMapping(value = "/solve-1")
    public ResponseEntity<Solution> solve(HttpServletResponse response, @RequestBody InputData inputData) {

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

        double[] costs = DataFileReader.readCosts();
        double[] prices = DataFileReader.readPrices();
        double[] spaces = DataFileReader.readSpaces();
        double[][] rotations = DataFileReader.readRotation();

        double[] profit = new double[costs.length];

        for (int i = 0; i < costs.length; i++) {
            profit[i] = prices[i] - costs[i];
        }

        DataFileReader.printArray(costs);
        DataFileReader.printArray(prices);
        DataFileReader.printArray(profit);
        DataFileReader.printMatrix(rotations);
        DataFileReader.printArray(spaces);

        d = GLPK.new_doubleArray(1000);
        ia = GLPK.new_intArray(1000);
        ja = GLPK.new_intArray(1000);

        for (int i = 1; i <= costs.length; i++) {
            GLPK.doubleArray_setitem(d, i, costs[i - 1]);
            GLPK.intArray_setitem(ia, i, 1);
            GLPK.intArray_setitem(ja, i, i);
        }

        for (int i = 1; i <= spaces.length; i++) {
            int absIndex = i + costs.length;
            GLPK.doubleArray_setitem(d, absIndex, spaces[i - 1]);
            GLPK.intArray_setitem(ia, absIndex, 2);
            GLPK.intArray_setitem(ja, absIndex, i);
        }

        double maxInvest = inputData.getBudget();
        GLPK.glp_add_rows(lp, 2);
        GLPK.glp_set_row_name(lp, 1, "c");
        GLPK.glp_set_row_bnds(lp, 1, GLPKConstants.GLP_UP, 0.0, maxInvest);

        double maxSpace = inputData.getSpace();
        GLPK.glp_set_row_name(lp, 2, "s");
        GLPK.glp_set_row_bnds(lp, 2, GLPKConstants.GLP_UP, 0.0, maxSpace);

        GLPK.glp_add_cols(lp, prices.length);

        for (int i = 0; i < profit.length; i++) {
            GLPK.glp_set_col_name(lp, i + 1, "x" + (i + 1));
            GLPK.glp_set_col_bnds(lp, i + 1, GLPKConstants.GLP_LO, 0.0, 0.0);
            GLPK.glp_set_obj_coef(lp, i + 1, profit[i]);
        }

        double[] solution = new double[prices.length];

        GLPK.glp_load_matrix(lp, prices.length * 2, ia, ja, d);
        printSystem(lp);
        GLPK.glp_simplex(lp, null);
        double z = GLPK.glp_get_obj_val(lp);

        Solution solutionObject = new Solution();
        List<SolutionCrop> solutionCrops = new ArrayList<SolutionCrop>();

        solutionObject.setSolutionCrops(solutionCrops);

        for (int i = 0; i < prices.length; i++) {
            solution[i] = GLPK.glp_get_col_prim(lp, i + 1);
            Crop crop = new Crop("couve", prices[i], costs[i], 1.0, 1.0);
            solutionCrops.add(new SolutionCrop((int) solution[i], solutionObject, crop, crop.getPrice(), crop.getSpace(), crop.getTime(), crop.getCost()));
        }

        //solutionService.save(solutionObject);
        System.out.println("z = " + z + "\n");
        DataFileReader.printArray(solution);

        GLPK.glp_delete_prob(lp);

        return ResponseEntity.ok(solutionObject);
    }


    @PostMapping(value = "/solve")
    public ResponseEntity<SolutionDTO> solveUsingDB(HttpServletResponse response, @RequestBody InputData inputData) {

        glp_prob lp;
        SWIGTYPE_p_int ia;
        SWIGTYPE_p_int ja;
        SWIGTYPE_p_double d;

        lp = GLPK.glp_create_prob();
        d = GLPK.new_doubleArray(1000);
        ia = GLPK.new_intArray(1000);
        ja = GLPK.new_intArray(1000);
        GLPK.glp_set_prob_name(lp, "sample");
        GLPK.glp_set_obj_dir(lp, GLPKConstants.GLP_MAX);

        List<Crop> cropList = inputData.getSelectedCrops();
        GLPK.glp_add_cols(lp, cropList.size());

        for (int i = 0; i < cropList.size(); i++) {

            double cost = cropList.get(i).getCost();
            double space = cropList.get(i).getSpace();
            double profit = cropList.get(i).getProfit();

            GLPK.doubleArray_setitem(d, i + 1, cost);
            GLPK.intArray_setitem(ia, i + 1, 1);
            GLPK.intArray_setitem(ja, i + 1, i + 1);

            GLPK.doubleArray_setitem(d, cropList.size() + i + 1, space);
            GLPK.intArray_setitem(ia, cropList.size() + i + 1, 2);
            GLPK.intArray_setitem(ja, cropList.size() + i + 1, i + 1);

            GLPK.glp_set_col_name(lp, i + 1, "x" + (i + 1));
            GLPK.glp_set_col_kind(lp, i + 1, GLPKConstants.GLP_IV);
            GLPK.glp_set_col_bnds(lp, i + 1, GLPKConstants.GLP_LO, 0.0, 0.0);
            GLPK.glp_set_obj_coef(lp, i + 1, profit);
        }

        double maxInvest = inputData.getBudget();
        double maxSpace = inputData.getSpace();
        GLPK.glp_add_rows(lp, 2);

        GLPK.glp_set_row_name(lp, 1, "c");
        GLPK.glp_set_row_bnds(lp, 1, GLPKConstants.GLP_UP, 0.0, maxInvest);

        GLPK.glp_set_row_name(lp, 2, "s");
        GLPK.glp_set_row_bnds(lp, 2, GLPKConstants.GLP_UP, 0.0, maxSpace);

        double[] solution = new double[cropList.size()];
        GLPK.glp_load_matrix(lp, cropList.size() * 2, ia, ja, d);
        printSystem(lp);
        GLPK.glp_simplex(lp, null);
        GLPK.glp_intopt(lp, null);
        double z = GLPK.glp_mip_obj_val(lp);

        Solution solutionObject = new Solution();
        List<SolutionCrop> solutionCrops = new ArrayList<SolutionCrop>();

        solutionObject.setSolutionCrops(solutionCrops);
        solutionObject.setInputData(inputData);
        inputData.setSolution(solutionObject);
        //solutionService.save(solutionObject);

        //inputDataService.save(inputData);

        for (int i = 0; i < cropList.size(); i++) {
            double amount = GLPK.glp_mip_col_val(lp, i + 1);
            Crop crop = cropList.get(i);
            SolutionCrop solutionCrop = new SolutionCrop((int) amount, solutionObject, crop, crop.getPrice(), crop.getSpace(), crop.getTime(), crop.getCost());
            solutionCrops.add(solutionCrop);

            solution[i] = amount;
        }

        solutionService.save(solutionObject);
        System.out.println("z = " + z + "\n");
        DataFileReader.printArray(solution);
        GLPK.glp_delete_prob(lp);
        return ResponseEntity.ok(new SolutionDTO(solutionObject));
    }


    @PostMapping(value = "/solve-new")
    public ResponseEntity<SolutionDTO> solveUsingDB2(HttpServletResponse response, @RequestBody InputData inputData) {

        glp_prob lp;
        SWIGTYPE_p_int ia;
        SWIGTYPE_p_int ja;
        SWIGTYPE_p_double d;

        lp = GLPK.glp_create_prob();
        d = GLPK.new_doubleArray(1000);
        ia = GLPK.new_intArray(1000);
        ja = GLPK.new_intArray(1000);
        GLPK.glp_set_prob_name(lp, "sample");
        GLPK.glp_set_obj_dir(lp, GLPKConstants.GLP_MAX);

        List<Crop> cropList = inputData.getSelectedCrops();
        GLPK.glp_add_cols(lp, cropList.size()*3*3);

       /* for (int i = 0; i < cropList.size(); i++) {

            double cost = cropList.get(i).getCost();
            double space = cropList.get(i).getSpace();
            double profit = cropList.get(i).getProfit();

            GLPK.doubleArray_setitem(d, i + 1, cost);
            GLPK.intArray_setitem(ia, i + 1, 1);
            GLPK.intArray_setitem(ja, i + 1, i + 1);

            GLPK.doubleArray_setitem(d, cropList.size() + i + 1, space);
            GLPK.intArray_setitem(ia, cropList.size() + i + 1, 2);
            GLPK.intArray_setitem(ja, cropList.size() + i + 1, i + 1);

            GLPK.glp_set_col_name(lp, i + 1, "x" + (i + 1));
            GLPK.glp_set_col_kind(lp, i + 1, GLPKConstants.GLP_IV);
            GLPK.glp_set_col_bnds(lp, i + 1, GLPKConstants.GLP_LO, 0.0, 0.0);
            GLPK.glp_set_obj_coef(lp, i + 1, profit);
        }*/

        List<Field> fields = new ArrayList<Field>();
        fields.add(new Field(null, 10, "a1"));
        fields.add(new Field(null, 10, "a2"));
        fields.add(new Field(null, 10, "a3"));


        int index=1;
        int fieldRestrictionCounter=1;
        int rotationRestrictionCounter=1;
        int matrixCounter=0;
        int M = 12;

        GLPK.glp_add_rows(lp, 1 + 3*3);

        for(int k=0; k< 3; k++){
            for(int j=0; j<3; j++){

                double fieldSize = fields.get(j).getSize();
                for(int i=0; i<cropList.size(); i++){
                    System.out.print(i + "\t");

                    double profit = cropList.get(i).getProfit();
                    double cost = cropList.get(i).getCost();
                    double space = cropList.get(i).getSpace();
                    double time = cropList.get(i).getTime();
                    double fieldProfit = (fieldSize/space)*profit;
                    double fieldCost = (fieldSize/space)*cost;

                    GLPK.doubleArray_setitem(d, index, fieldCost);
                    GLPK.intArray_setitem(ia, index, 1);
                    GLPK.intArray_setitem(ja, index, index);



                    GLPK.doubleArray_setitem(d, index+27, 1.0);
                    GLPK.intArray_setitem(ia, index+27, 1+fieldRestrictionCounter);
                    GLPK.intArray_setitem(ja, index+27, index);


                    /*GLPK.doubleArray_setitem(d, index+54, -(M*time));
                    GLPK.intArray_setitem(ia, index+54, 9+1+rotationRestrictionCounter);
                    GLPK.intArray_setitem(ja, index+54, 0);

                    for(int x=1; x<=time; x++){
                        GLPK.doubleArray_setitem(d, x+index+54, M);
                        GLPK.intArray_setitem(ia, x+index+54, 9+1+rotationRestrictionCounter);
                        GLPK.intArray_setitem(ja, x+index+54, index);
                    }

                    GLPK.glp_set_row_name(lp, 9+1+rotationRestrictionCounter, "r"+rotationRestrictionCounter);
                    GLPK.glp_set_row_bnds(lp, 9+1+rotationRestrictionCounter, GLPKConstants.GLP_DB, 0, 0);
*/
                    rotationRestrictionCounter++;

                    matrixCounter+=2;

                    GLPK.glp_set_col_name(lp, (i+1)+3*(j)+9*(k), "x" + (i + 1) + "," + (j + 1) +"," + (k + 1));
                    GLPK.glp_set_col_kind(lp, (i+1)+3*(j)+9*(k), GLPKConstants.GLP_BV);
                    GLPK.glp_set_col_bnds(lp, (i+1)+3*(j)+9*(k), GLPKConstants.GLP_DB, 0, 1);
                    GLPK.glp_set_obj_coef(lp, (i+1)+3*(j)+9*(k), fieldProfit);

                    index++;

                }

                GLPK.glp_set_row_name(lp, 1+fieldRestrictionCounter, "j"+fieldRestrictionCounter);
                GLPK.glp_set_row_bnds(lp, 1+fieldRestrictionCounter, GLPKConstants.GLP_DB, 0, 1);

                fieldRestrictionCounter++;

                System.out.println();
            }
            System.out.println("\n\n");
        }


        double maxInvest = inputData.getBudget();
       // double maxSpace = inputData.getSpace();

        GLPK.glp_set_row_name(lp, 1, "c");
        GLPK.glp_set_row_bnds(lp, 1, GLPKConstants.GLP_DB, 0.0, maxInvest);

        //GLPK.glp_set_row_name(lp, 2, "s");
        //GLPK.glp_set_row_bnds(lp, 2, GLPKConstants.GLP_UP, 0.0, maxSpace);

        double[] solution = new double[27];
        GLPK.glp_load_matrix(lp, 54, ia, ja, d);
        //printSystem(lp);
        GLPK.glp_write_lp(lp, null, "my_ourput.txt");
        GLPK.glp_simplex(lp, null);
        GLPK.glp_intopt(lp, null);
        double z = GLPK.glp_mip_obj_val(lp);

        Solution solutionObject = new Solution();
        List<SolutionCrop> solutionCrops = new ArrayList<SolutionCrop>();

        solutionObject.setSolutionCrops(solutionCrops);
        solutionObject.setInputData(inputData);
        inputData.setSolution(solutionObject);
        //solutionService.save(solutionObject);

        //inputDataService.save(inputData);

        for (int i = 0; i < solution.length; i++) {
            double value = GLPK.glp_mip_col_val(lp, i + 1);
            String varName = GLPK.glp_get_col_name(lp, i+1);

            if(value!=0.0){
                String commaSeparatedIndices = varName.substring(1);
                String[] indices = commaSeparatedIndices.split(",");
                int ii = Integer.parseInt(indices[0]);
                int ij = Integer.parseInt(indices[1]);
                int ik = Integer.parseInt(indices[2]);
                System.out.println(varName);
                System.out.println("Plantar " + cropList.get(ii-1).getName() + ", em " + fields.get(ij-1).getName() + " no tempo "+ (ik-1));
            }
            //Crop crop = cropList.get(i);
           // SolutionCrop solutionCrop = new SolutionCrop((int) amount, solutionObject, crop, crop.getPrice(), crop.getSpace(), crop.getTime(), crop.getCost());
            //solutionCrops.add(solutionCrop);

            solution[i] = value;
        }

        //solutionService.save(solutionObject);
        System.out.println("z = " + z + "\n");
        DataFileReader.printArray(solution);
        GLPK.glp_delete_prob(lp);
        return ResponseEntity.ok(new SolutionDTO(solutionObject));
    }


    public void printSystem(glp_prob lp) {
        int numCols = GLPK.glp_get_num_cols(lp) + 1;
        int numRows = GLPK.glp_get_num_rows(lp);

        System.out.print("z \t");

        for (int i = 0; i < numCols; i++) {
            double coef = GLPK.glp_get_obj_coef(lp, i);

            if (i == 0) {
                System.out.print("c: " + coef + "\t");
            } else {
                System.out.print("x" + i + ": " + coef + "\t ");
            }

        }
        System.out.println();
        for (int i = 1; i <= numRows; i++) {
            printRow(lp, i);
        }

    }

    public void printRow(glp_prob lp, int rowIndex) {

        int numCols = GLPK.glp_get_num_cols(lp) + 1;
        SWIGTYPE_p_int ind = GLPK.new_intArray(numCols + 1);
        SWIGTYPE_p_double d = GLPK.new_doubleArray(numCols + 1);

        GLPK.glp_get_mat_row(lp, rowIndex, ind, d);

        System.out.print(GLPK.glp_get_row_name(lp, rowIndex) + "\t");

        for (int i = 0; i < numCols; i++) {

            int itemIndex = GLPK.intArray_getitem(ind, i);
            double value = GLPK.doubleArray_getitem(d, itemIndex);

            if (i == 0) {
                System.out.print("c: " + value + "\t");
            } else {

                System.out.print(GLPK.glp_get_col_name(lp, i) + ": " + value + "\t ");
            }
        }

        double upper = GLPK.glp_get_row_ub(lp, rowIndex);
        double lower = GLPK.glp_get_row_lb(lp, rowIndex);

        System.out.print("[" + lower + ", " + upper + "]");

        System.out.println();

    }

    @PostMapping(value = "/test")
    public ResponseEntity<Solution> test(HttpServletResponse response, @RequestBody InputData inputData) {

        System.out.println(inputData.getBudget());

        Solution solution = new Solution();
       /* List<Crop> cropList = new ArrayList<Crop>();
        cropList.add(new Crop(1.0, 0.5, 50));
        cropList.add(new Crop(1.0, 0.5, 50));
        cropList.add(new Crop(1.0, 0.5, 50));
        cropList.add(new Crop(1.0, 0.5, 50));
        cropList.add(new Crop(1.0, 0.5, 50));
        cropList.add(new Crop(1.0, 0.5, 50));

        solution.setCrops(cropList);*/

        return ResponseEntity.ok(solution);
    }


}
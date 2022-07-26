package com.example.agricultureoptimizerbackend.model;

import com.example.agricultureoptimizerbackend.dto.InputDataDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="input_data")
public class InputData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "budget", nullable = false)
    private double budget;
    @Column(name = "space", nullable = false)
    private double space;
    @Column(name = "time_frames", nullable = false)
    private int timeFrames;

    @JsonIgnore
    @OneToOne(mappedBy = "inputData")
    private Solution solution;

    @Transient
    @OneToMany
    private List<Crop> selectedCrops;

    @Transient
    @OneToMany
    private List<Field> fields;


    public Solution getSolution() {
        return solution;
    }
    //private List<Double> plots;
    //private List<Crop> crops;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InputData() {
    }

    public InputData(InputDataDTO dto) {
        this.id = dto.getId();
        this.budget = dto.getBudget();
        this.space = dto.getSpace();
        this.timeFrames = dto.getTimeFrames();

    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public double getSpace() {
        return space;
    }

    public void setSpace(double space) {
        this.space = space;
    }

    public void setSolution(Solution solution) {
        this.solution = solution;
    }

    public List<Crop> getSelectedCrops() {
        return selectedCrops;
    }

    public void setSelectedCrops(List<Crop> selectedCrops) {
        this.selectedCrops = selectedCrops;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public int getTimeFrames() {
        return timeFrames;
    }

    public void setTimeFrames(int timeFrames) {
        this.timeFrames = timeFrames;
    }
}

package com.example.agricultureoptimizerbackend.services;

import com.example.agricultureoptimizerbackend.dto.CropDTO;
import com.example.agricultureoptimizerbackend.dto.InputDataDTO;
import com.example.agricultureoptimizerbackend.dto.SolutionDTO;
import com.example.agricultureoptimizerbackend.model.Crop;
import com.example.agricultureoptimizerbackend.model.InputData;
import com.example.agricultureoptimizerbackend.repositories.CropRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CropService {

    @Autowired
    private CropRepository repository;

    @Transactional(readOnly = true)
    public List<CropDTO> findAll(){
       List<Crop> result = repository.findAll();

       return result.stream().map(CropDTO::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Crop> findAllEntities(){
        List<Crop> result = repository.findAll();

        return result;
    }

    @Transactional(readOnly = true)
    public CropDTO findById(Long id){
        Crop result = repository.findById(id).orElse(null);
        if(result == null){
            return null;
        }
        return new CropDTO(result);
    }

    @Transactional
    public Crop save(Crop entity){
        return repository.save(entity);
    }

    @Transactional
    public Crop save(CropDTO dto){
        return repository.save(new Crop(dto));
    }

    }

package vttp2022.project.addressprocessor.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import vttp2022.project.addressprocessor.exceptions.UserAlreadyExistException;
import vttp2022.project.addressprocessor.models.File;
import vttp2022.project.addressprocessor.repositories.FilesRepository;
import vttp2022.project.addressprocessor.repositories.UsersRepository;

//contains all the authentication methods
@Service
public class UserService {
    
    @Autowired private UsersRepository usersRepo;

    @Autowired private FilesRepository filesRepo;

    //user login authentications
    
    public boolean validateLogin(String email, String password) {
        return usersRepo.validateLogin(email, password);
    }

    public boolean registerUser(String email, String password) throws UserAlreadyExistException{
        if (usersRepo.checkUserExist(email)) {
            throw new UserAlreadyExistException();
        }
         
        return usersRepo.registerUser(email, password);
    }
    
    //end user login authentications

    //user file methods
    
    public void saveUserFile(String email, String queryName, String generatedFilename) {
        filesRepo.insertFilenames(email, queryName, generatedFilename);
    }
    
    public List<File> retrieveUserFiles(String email) {
        return filesRepo.retrieveFilenames(email);
    }

    public boolean deleteFile(String fileToDelete) {
        return filesRepo.deleteFile(fileToDelete);
    }
    
    //end user file methods
}

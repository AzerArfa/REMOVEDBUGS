package com.auth.services.auth;

import com.auth.dto.SignupRequest;
import com.auth.dto.UserDto;
import com.auth.entity.Entreprise;
import com.auth.entity.Password;
import com.auth.entity.Role;
import com.auth.entity.User;
import com.auth.exceptions.UserNotFoundException;
import com.auth.repository.PasswordRepository;
import com.auth.repository.RoleRepository;
import com.auth.repository.UserRepository;
import com.auth.dto.ChangePasswordDto;
import com.auth.dto.EntrepriseDto;
import com.auth.dto.RoleDto;

import jakarta.annotation.PostConstruct;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private EntrepriseService entrepriseService;
    @Autowired
    private PasswordRepository passwordRepository;
    public UserDto getUserByEmail(String email) throws UserNotFoundException {
        Optional<User> userOptional = userRepository.findFirstByEmail(email);
        if (userOptional.isPresent()) {
            return convertToUserDto(userOptional.get());
        } else {
            throw new UserNotFoundException("No user found with email: ");
        }
    }
    @PostConstruct
    public void testFindFirstByEmail() {
        Optional<User> user = userRepository.findFirstByEmail("image22@gmail.com");
        if (user.isPresent()) {
            System.out.println("User found: " + user.get().getEmail());
        } else {
            System.out.println("No user found with that email.");
        }
    }
    
    @Transactional
    public UserDto convertToUserDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(userDto.getId());
        userDto.setName(userDto.getName());
        userDto.setPrenom(userDto.getPrenom());
        userDto.setEmail(userDto.getEmail());
        userDto.setImg(userDto.getImg());
        List<EntrepriseDto> entrepriseDtos = user.getEntreprises().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        userDto.setEntreprises(entrepriseDtos);
        userDto.setCreationDate(userDto.getCreationDate());
        List<RoleDto> roleDtos = userDto.getRoles().stream()
                .map(role -> {
                    RoleDto roleDto = new RoleDto();
                    roleDto.setId(role.getId());
                    roleDto.setName(role.getName());
                    return roleDto;
                })
                .collect(Collectors.toList());
userDto.setRoles(roleDtos);
        return userDto;
    }
    @Override
    public boolean deleteUserById(UUID userId) throws UserNotFoundException {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
        return true;
    }

    @Transactional
    public UserDto createUser(SignupRequest signupRequest) {
        Password initialPassword = new Password();

        // Email validation
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(signupRequest.getEmail());

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // CIN validation
        String cin = signupRequest.getCin();
        if (cin == null || !cin.matches("\\d{8}")) {
            throw new IllegalArgumentException("CIN must be exactly 8 digits");
        }

        // Password length validation
        String password = signupRequest.getPassword();
        if (password == null || password.length() != 8) {
            throw new IllegalArgumentException("Password must be exactly 8 characters long");
        }

        // Date of birth validation
        Date datenais = signupRequest.getDatenais();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -18);
        Date cutoffDate = calendar.getTime();
        if (datenais == null || !datenais.before(cutoffDate)) {
            throw new IllegalArgumentException("User must be older than 18 years");
        }

        User user = new User();
        user.setEmail(signupRequest.getEmail());
        user.setName(signupRequest.getName());
        user.setPrenom(signupRequest.getPrenom());
        user.setImg(signupRequest.getImg());
        user.setCin(signupRequest.getCin());
        user.setDatenais(signupRequest.getDatenais());
        user.setLieunais(signupRequest.getLieunais());

        initialPassword.setPassword(new BCryptPasswordEncoder().encode(signupRequest.getPassword()));
        initialPassword.setCreationDate(new Date());
        initialPassword.setUser(user);
        user.setPasswords(Collections.singletonList(initialPassword));  // Set the password as a list containing one element

        Role userRole = roleRepository.findByName("ROLE_USER");
        if (userRole == null) {
            userRole = new Role();
            userRole.setName("ROLE_USER");
            roleRepository.save(userRole);
        }
        user.getRoles().add(userRole);
        User createdUser = userRepository.save(user);
        passwordRepository.save(initialPassword);
        return createdUser.getUserDto();
    }

    @Transactional
    public ResponseEntity<?> updatePasswordById(ChangePasswordDto changePasswordDto) {
    	Password newPassword = new Password();
        try {
            Optional<User> userOptional = userRepository.findById(changePasswordDto.getId());
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                // Retrieve the most recent password
                Password latestPassword = user.getPasswords().stream()
                    .max(Comparator.comparing(Password::getCreationDate))
                    .orElse(null);

                if (latestPassword != null && this.bCryptPasswordEncoder.matches(changePasswordDto.getOldPassword(), latestPassword.getPassword())) {
                    // Ensure the new password has not been used previously
                    boolean isUnique = user.getPasswords().stream()
                        .noneMatch(p -> bCryptPasswordEncoder.matches(changePasswordDto.getNewPassword(), p.getPassword()));

                    if (!isUnique) {
                        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("New password cannot be the same as any previous passwords.");
                    }

                    // Encode new password and add to the passwords list
                   
                    newPassword.setPassword(bCryptPasswordEncoder.encode(changePasswordDto.getNewPassword()));
                    newPassword.setCreationDate(new Date());
                    newPassword.setUser(user);
                    user.getPasswords().add(newPassword);

                    userRepository.save(user);
                    UserDto userDto = new UserDto();
                    userDto.setId(user.getId());
                    return ResponseEntity.status(HttpStatus.OK).body(userDto);
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Old password is incorrect.");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
        }
    }
    @Transactional
    @Override
    public ResponseEntity<?> updateUserById(UserDto userDto) {
        try {
            Optional<User> userOptional = userRepository.findById(userDto.getId());
            if (!userOptional.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            User user = userOptional.get();
            user.setName(userDto.getName());
            user.setPrenom(userDto.getPrenom());
            user.setEmail(userDto.getEmail());
            user.setImg(userDto.getImg());
            user.setCin(userDto.getCin());
            user.setDatenais(userDto.getDatenais());
            user.setLieunais(userDto.getLieunais());

            User updatedUser = userRepository.save(user);

            UserDto updatedUserDto = new UserDto();
            updatedUserDto.setId(updatedUser.getId());
            updatedUserDto.setName(updatedUser.getName());
            updatedUserDto.setPrenom(updatedUser.getPrenom());
            updatedUserDto.setEmail(updatedUser.getEmail());
            updatedUserDto.setImg(updatedUser.getImg());
            updatedUserDto.setCin(updatedUser.getCin());
            updatedUserDto.setDatenais(updatedUser.getDatenais());
            updatedUserDto.setLieunais(updatedUser.getLieunais());
            // Populate other fields as necessary

            return ResponseEntity.status(HttpStatus.OK).body(updatedUserDto);
        } catch (Exception e) {
            // Consider logging the exception here
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
        }
    }
    @Override
    public List<UserDto> searchUsersByName(String partialName) {
    	List<User> users =userRepository.findByNameContainingIgnoreCase(partialName);
        return users.stream().map(User::getUserDto).collect(Collectors.toList());
    }


    public Boolean hasUserWithEmail(String email) {
        return userRepository.findFirstByEmail(email).isPresent();
    }

    @Transactional
    @Override
    public UserDto getUserById(UUID id) {
        System.out.println("Fetching user with ID: " + id);
        Optional<User> optionalUser = userRepository.findById(id);
        return optionalUser.map(user -> {
            System.out.println("User found: " + user.getUsername());
            return convertToDto(user);
        }).orElseThrow(() -> {
            System.out.println("User not found with ID: " + id);
            return new RuntimeException("User not found");
        });
    }

    @Transactional
    @Override
    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(this::convertToDto).collect(Collectors.toList());
    }
    @Transactional
    @Override
    public void addEntrepriseToUser(UUID userId, EntrepriseDto entrepriseDto) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Entreprise entreprise = entrepriseService.createEntreprise(entrepriseDto);
        user.getEntreprises().add(entreprise); // assuming getter and setter are properly set up
        userRepository.save(user);
    }


    @Transactional
    public UserDto makeAdmin(UUID id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            Role adminRole = roleRepository.findByName("ADMIN");
            user.getRoles().add(adminRole);
            return userRepository.save(user).getUserDto();
        }
        return null;
    }

    @Transactional
    public UserDto makeUser(UUID id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            Role userRole = roleRepository.findByName("ROLE_USER");
            user.getRoles().add(userRole);
            return userRepository.save(user).getUserDto();
        }
        return null;
    }

    public boolean checkIfPasswordNeedsUpdate(User user) {
        if (user.getPasswords() == null || user.getPasswords().isEmpty()) {
            return false; // If there are no passwords, no need to update
        }

        // Find the most recent password's creation date
        Date lastPasswordCreationDate = user.getPasswords().stream()
            .max(Comparator.comparing(Password::getCreationDate))
            .map(Password::getCreationDate)
            .orElse(new Date(0)); // This defaults to a very old date if somehow no dates are found

        long differenceInMilliseconds = new Date().getTime() - lastPasswordCreationDate.getTime();
        long differenceInDays = differenceInMilliseconds / (1000 * 60 * 60 * 24);

        return differenceInDays >= 30;
    }

    @PostConstruct
    public void createAdminAccount() {
        Role superAdminRole = roleRepository.findByName("ROLE_SUPERADMIN");
        boolean hasSuperAdmin = superAdminRole != null && superAdminRole.getUsers() != null && !superAdminRole.getUsers().isEmpty();
        Password password = new Password();
        if (!hasSuperAdmin) {
            User user = new User();
            user.setEmail("superadmin@test.com");
            user.setName("superadmin");

            // Create a new Password object
            
            password.setPassword(new BCryptPasswordEncoder().encode("superadmin"));
            password.setCreationDate(new Date()); // Set current date as creation date
            password.setUser(user);
           
            // Setting the password to the user
            user.setPasswords(Collections.singletonList(password)); // Assuming setPasswords takes a list

            if (superAdminRole == null) {
                superAdminRole = new Role();
                superAdminRole.setName("ROLE_SUPERADMIN");
                roleRepository.save(superAdminRole);
            }
            user.getRoles().add(superAdminRole);
            userRepository.save(user);
            passwordRepository.save(password);
        }
        
    }
    @Transactional
    @Override
    public List<EntrepriseDto> getEntreprisesByUserId(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(
            () -> new RuntimeException("User not found"));
        Hibernate.initialize(user.getEntreprises());
        return user.getEntreprises().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }


    private UserDto convertToDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setName(user.getName());
        userDto.setPrenom(user.getPrenom());
        userDto.setEmail(user.getEmail());
        userDto.setImg(user.getImg());
        userDto.setDatenais(user.getDatenais());
        userDto.setLieunais(user.getLieunais());
        userDto.setCin(user.getCin());
        List<EntrepriseDto> entrepriseDtos = user.getEntreprises().stream()
                .map(entreprise -> {
                    EntrepriseDto entrepriseDto = new EntrepriseDto();
                    entrepriseDto.setId(entreprise.getId());
                    entrepriseDto.setCodeTVA(entreprise.getCodeTVA());
                    entrepriseDto.setName(entreprise.getNom());
                    entrepriseDto.setAdresse(entreprise.getAdresse());
                    entrepriseDto.setSecteuractivite(entreprise.getSecteuractivite());
                    entrepriseDto.setMatricule(entreprise.getMatricule());
                    entrepriseDto.setVille(entreprise.getVille());
                    entrepriseDto.setSiegesociale(entreprise.getSiegesociale());
                    entrepriseDto.setLogo(entreprise.getLogo());
                    return entrepriseDto;
                })
                .collect(Collectors.toList());
        userDto.setEntreprises(entrepriseDtos);
        List<RoleDto> roleDtos = user.getRoles().stream()
                .map(role -> {
                    RoleDto roleDto = new RoleDto();
                    roleDto.setId(role.getId());
                    roleDto.setName(role.getName());
                    return roleDto;
                })
                .collect(Collectors.toList());
userDto.setRoles(roleDtos);
        return userDto;
    }
    private EntrepriseDto convertToDto(Entreprise entreprise) {
        EntrepriseDto dto = new EntrepriseDto();
        dto.setId(entreprise.getId());
        dto.setName(entreprise.getNom());
        dto.setAdresse(entreprise.getAdresse());
        dto.setSecteuractivite(entreprise.getSecteuractivite());
        dto.setMatricule(entreprise.getMatricule());
        dto.setVille(entreprise.getVille());
        dto.setSiegesociale(entreprise.getSiegesociale());
        dto.setCodeTVA(entreprise.getCodeTVA());
         // Make sure this field is correctly set in the entity
        return dto;
    }


    private UserDto convertUserToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setPrenom(user.getPrenom());
        dto.setCin(user.getCin());
        dto.setDatenais(user.getDatenais());
        dto.setLieunais(user.getLieunais());
        dto.setRoles(user.getRoles().stream().map(this::convertRoleToDto).collect(Collectors.toList()));
        dto.setReturnedImg(user.getImg());
        dto.setEntreprises(user.getEntreprises().stream().map(this::convertToDto).collect(Collectors.toList()));

        return dto;
    }

    private RoleDto convertRoleToDto(Role role) {
        RoleDto roleDto = new RoleDto();
        roleDto.setId(role.getId());
        roleDto.setName(role.getName());
        // Populate other necessary fields
        return roleDto;
    }


}

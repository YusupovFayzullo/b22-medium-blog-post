package uz.jl.blogpost.backend.services;

import lombok.NonNull;
import uz.jl.blogpost.backend.configs.ApplicationContext;
import uz.jl.blogpost.backend.criteria.UserCriteria;
import uz.jl.blogpost.backend.daos.GenericDAO;
import uz.jl.blogpost.backend.daos.MailDao;
import uz.jl.blogpost.backend.daos.UserDAO;
import uz.jl.blogpost.backend.domains.MailReciever;
import uz.jl.blogpost.backend.domains.User;
import uz.jl.blogpost.backend.dtos.user.LoginRequest;
import uz.jl.blogpost.backend.dtos.user.UserCreateDTO;
import uz.jl.blogpost.backend.dtos.user.UserDTO;
import uz.jl.blogpost.backend.dtos.user.UserUpdateDTO;
import uz.jl.blogpost.backend.mappers.UserMapper;
import uz.jl.blogpost.backend.response.DataDTO;
import uz.jl.blogpost.backend.response.ErrorDTO;
import uz.jl.blogpost.backend.response.Response;
import uz.jl.blogpost.backend.services.base.AbstractService;
import uz.jl.blogpost.backend.services.mail.MailService;
import uz.jl.blogpost.backend.utils.BaseUtil;
import uz.jl.blogpost.backend.utils.validators.UserValidator;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public class UserServiceImpl extends AbstractService<UserDAO, UserMapper, UserValidator> implements UserService {

    private static UserService service;
    private final MailService mailService = ApplicationContext.getBean(MailService.class);

    private final Logger logger = Logger.getLogger(getClass().getName());

    public UserServiceImpl(UserDAO dao, UserMapper mapper, UserValidator validator) {
        super(dao, mapper, validator);
    }

    @Override
    public Response<DataDTO<String>> create(@NonNull UserCreateDTO dto) {
        try {
            validator.checkOnCreate(dto);
            User user = mapper.fromCreateDTO(dto);
            user.setPassword(util.encode(user.getPassword()));
            user.setId(BaseUtil.generateUniqueID());
            dao.save(user);
            return new Response<>(new DataDTO<>(user.getId()));
        } catch (IllegalArgumentException e) {
            logger.severe(e.getLocalizedMessage());
            ErrorDTO error = new ErrorDTO(e.getCause());
            return new Response<>(new DataDTO<>(error));
        }
    }

    @Override
    public Response<DataDTO<Boolean>> update(@NonNull UserUpdateDTO dto) {

        validator.checkID(dto.getId());
        Optional<User> userOptional = dao.findById(dto.getId());
        if (userOptional.isEmpty())
            return new Response<>(new DataDTO<>(new ErrorDTO("User not found by id :%s".formatted(dto.getId()))));
        User user = userOptional.get();
        user.setUsername(Objects.requireNonNullElse(dto.getUsername(), user.getUsername()));
        user.setFullName(Objects.requireNonNullElse(dto.getFullName(), user.getFullName()));
        user.setLanguage(Objects.requireNonNullElse(User.Language.getByName(dto.getLanguage()), user.getLanguage()));
        user.setEmail(Objects.requireNonNullElse(dto.getEmail(), user.getEmail()));
        user.setUpdatedAt(LocalDateTime.now(Clock.system(ZoneId.of("Asia/Tashkent"))));
        return new Response<>(new DataDTO<>(dao.update(user)));

    }

    public Response<DataDTO<Boolean>> forgetPassword(@NonNull String email) {
        Optional<User> userOptional = dao.findByEmail(email);
        if (userOptional.isEmpty())
            return new Response<>(new DataDTO<>(new ErrorDTO("User not found with email %s".formatted(email))));
        User user = userOptional.get();
        String uniqueCode = BaseUtil.generateUniqueID();
        Map<String, String> body = Map.of(
                "subject", "Confirmation Code",
                "content", uniqueCode,
                "email", user.getEmail()
        );
        mailService.sendEmail(body);

        MailReciever mailReciever=new MailReciever(user.getId(),uniqueCode,LocalDateTime.now(Clock.system(ZoneId.of("Asia/Tashkent"))));
        MailDao mailDao=new MailDao();
        mailDao.save(mailReciever);
        mailDao.shutDownHook();
        return new Response<>(new DataDTO<>(true));
    }

    public Response<DataDTO<Boolean>> forgetPasswordConfirmation() {

        // confirm and reset

        return null;
    }

    @Override
    public Response<DataDTO<Boolean>> delete(@NonNull String s) {
        return null;
    }

    @Override
    public Response<DataDTO<UserDTO>> get(@NonNull String s) {
        return null;
    }

    @Override
    public Response<DataDTO<List<UserDTO>>> getAll(@NonNull UserCriteria criteria) {
        return null;
    }

    @Override
    public Response<DataDTO<UserDTO>> login(@NonNull LoginRequest loginRequest) {
        try {
            User user = dao.findByUsername(loginRequest.username());
            if (!util.match(loginRequest.password(), user.getPassword()))
                return new Response<>(new DataDTO<>(new ErrorDTO("Bad credentials")));
            UserDTO userDTO = mapper.toDTO(user);
            return new Response<>(new DataDTO<>(userDTO));
        } catch (RuntimeException e) {
            // TODO: 08/12/22 need to logger here
            return new Response<>(new DataDTO<>(new ErrorDTO("Bad credentials")));
        }
    }

    public static UserService getInstance() {
        if (service == null) {
            service = new UserServiceImpl(
                    ApplicationContext.getBean(UserDAO.class),
                    ApplicationContext.getBean(UserMapper.class),
                    ApplicationContext.getBean(UserValidator.class)
            );
        }
        return service;
    }
}

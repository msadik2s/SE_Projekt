package org.carlook.process.control;

import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import org.carlook.gui.ui.MyUI;
import org.carlook.gui.windows.ConfirmationWindow;
import org.carlook.model.dao.RegisterDAO;
import org.carlook.model.dao.RoleDAO;
import org.carlook.model.dao.UserDAO;
import org.carlook.model.objects.dto.UserDTO;
import org.carlook.process.Interfaces.RegistrationControlInterface;
import org.carlook.process.exceptions.DatabaseException;
import org.carlook.process.exceptions.EmailInUseException;
import org.carlook.process.exceptions.EmptyFieldException;
import org.carlook.process.exceptions.NoEqualPasswordException;
import org.carlook.services.db.JDBCConnection;
import org.carlook.services.util.Roles;
import org.carlook.services.util.Views;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegistrationControl implements RegistrationControlInterface {

    private static RegistrationControl registration = null;
    private RegistrationControl(){
    }

    public static RegistrationControl getInstance(){
        if(registration == null){
            registration = new RegistrationControl();
        }
        return registration;
    }

    public void checkValid(String email, boolean emailBool, String password1, String password2, boolean password1Bool, boolean password2Bool, boolean checkBox) throws NoEqualPasswordException, DatabaseException, EmailInUseException, EmptyFieldException, SQLException {

        //Eingabecheck
        if (!emailBool || !password1Bool  || !password2Bool || !checkBox) {
            throw new EmptyFieldException("Bitte ergänzen Sie Ihre Eingaben in den makierten Bereichen!");
        }

        //Passwortcheck
        if ( !password1.equals(password2) ) {
            throw new NoEqualPasswordException("Passwörter stimmen nicht überein!");
        }

        //DB Zugriff Emailcheck
        String sql = "SELECT email " +
                     "FROM collhbrs.user " +
                     "WHERE email = ? ;";
        ResultSet rs = null;
        PreparedStatement statement = JDBCConnection.getInstance().getPreparedStatement(sql);

        try {
            statement.setString(1,email);
            rs = statement.executeQuery();
        } catch (SQLException throwables) {
            Notification.show("Es ist ein SQL-Fehler aufgetreten. Bitte informieren Sie einen Administrator!", Notification.Type.ERROR_MESSAGE);
        }

        try {
            assert rs != null;
            if (rs.next()) {
                throw new EmailInUseException("Die Email wird bereits benutzt!");
            }
        } catch (SQLException throwables) {
            throw new DatabaseException("Fehler bei set: Bitte den Programmierer informieren!");
        } finally {
            assert rs != null;
            rs.close();
        }
    }

    //User registrieren
    public void registerUser( String email, String password, String regAs ) throws DatabaseException, SQLException {
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail(email);
        userDTO.setPassword(password);
        boolean registerUser;
        RegisterDAO.getInstance().addUser(userDTO);
        userDTO.setId(UserDAO.getInstance().getMaxID());

        if (regAs.equals(Roles.STUDENT)) {
            RegisterDAO.getInstance().addStudent(userDTO);
            registerUser = RoleDAO.getInstance().setRolesForStudent(userDTO);
        } else {
            RegisterDAO.getInstance().addUnternehmen(userDTO);
            registerUser = RoleDAO.getInstance().setRolesForUnternehmen(userDTO);
        }

        if (registerUser) {
            UI.getCurrent().addWindow( new ConfirmationWindow("Registration erfolgreich!") );
            ( (MyUI)UI.getCurrent() ).setUserDTO(userDTO);
            UI.getCurrent().getNavigator().navigateTo(Views.MAIN);
        } else {
            throw new DatabaseException("Fehler bei Abschluß der Registration");
        }

    }

    //User Löschen
    public void deleteUser(UserDTO userDTO){
        RegisterDAO.getInstance().deleteUser(userDTO);
    }
}

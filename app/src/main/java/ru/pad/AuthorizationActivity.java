package ru.pad;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.pad.objects.GenericUser;

/**
 * Класс объекта активности авторизации
 */
public class AuthorizationActivity extends AppCompatActivity {
    /**
     * Регулярное выражение для определения корректности формата электронной почты
     */
    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z\\d._%+-]+@[A-Z\\d.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    /**
     * Определяет, корректный ли формат имеет электронная почта, используя регулярное выражение
     * <b>VALID_EMAIL_REGEX</b>
     * @param email электронная почта, формат которой нужно проверить
     * @return true, если формат электронной почты корректен, иначе - false
     * @see #VALID_EMAIL_ADDRESS_REGEX
     */
    public static boolean emailFormatIsValid(String email) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(email);
        return matcher.matches();
    }

    /**
     * Отображает эту активность на экране, вызывает метод <b>initWidgets()</b>
     * @param savedInstanceState сохраненное состояние активности
     * @see #initWidgets()
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorization);
        initWidgets();
    }

    /**
     * Инициализирует виджеты активности, реализует алгоритм авторизации пользователя
     */
    protected void initWidgets() {
        EditText editTextEmail = findViewById(R.id.editTextEmail);
        EditText editTextPassword = findViewById(R.id.editTextPassword);
        TextInputLayout inputLayoutEmail = findViewById(R.id.inputLayoutEmail);
        TextInputLayout inputLayoutPassword = findViewById(R.id.inputLayoutPassword);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        Button buttonAuthorization = findViewById(R.id.buttonAuthorization);
        Button buttonRestorePassword = findViewById(R.id.buttonRestorePassword);
        /*
          Привязка к кнопке функции авторизации пользователя
         */
        buttonAuthorization.setOnClickListener(unused1 -> {
            if (TextUtils.isEmpty(editTextEmail.getText().toString())) {
                inputLayoutEmail.setError("Введите вашу электронную почту");
                return;
            } else inputLayoutEmail.setError(null);

            if (!emailFormatIsValid(editTextEmail.getText().toString())) {
                inputLayoutEmail.setError("Некорректный формат электронной почты");
                return;
            }  else inputLayoutEmail.setError(null);

            if (TextUtils.isEmpty(editTextPassword.getText().toString())) {
                inputLayoutPassword.setError("Введите пароль");
                return;
            } inputLayoutEmail.setError(null);

            /*
              Попытка авторизации пользователя
             */
            auth.signInWithEmailAndPassword(editTextEmail.getText().toString(), editTextPassword.getText().toString())
                    /*
                      Если авторизация удалась, запуск следующей активности - профиля спортсмена/психолога
                     */
                    .addOnSuccessListener(unused2 -> {
                        String uid = editTextEmail.getText().toString().replace(".", "•");
                        database.getReference("Users/" + uid).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                GenericUser genericUser = dataSnapshot.getValue(GenericUser.class);

                                if (Objects.requireNonNull(genericUser).getRole().equals("Спортсмен")) {
                                    Intent sportsmanProfileActivity = new Intent(AuthorizationActivity.this, SportsmanProfileActivity.class);
                                    sportsmanProfileActivity.putExtra("sportsmanUid", uid);
                                    startActivity(sportsmanProfileActivity);
                                } else {
                                    Intent psychologistProfileActivity = new Intent(AuthorizationActivity.this, PsychologistProfileActivity.class);
                                    psychologistProfileActivity.putExtra("psychologistUid", uid);
                                    startActivity(psychologistProfileActivity);
                                }
                                finishAffinity();
                                finish();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(
                                        AuthorizationActivity.this,
                                        "Произошла ошибка при авторизации (" + error.getMessage() + ")",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    })
                    /*
                      Если авторизация не удалась, вывод пользователю соответствующего сообщения об ошибке
                     */
                    .addOnFailureListener(e -> {
                        if (Objects.requireNonNull(e.getMessage()).contains("There is no user record")) {
                            Toast.makeText(
                                    AuthorizationActivity.this,
                                    "Ошибка авторизации: указанный email-адрес не зарегистрирован",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                        else if (Objects.requireNonNull(e.getMessage()).contains("The password is invalid")) {
                            Toast.makeText(
                                    AuthorizationActivity.this,
                                    "Ошибка авторизации: неправильный пароль ",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                        else if (Objects.requireNonNull(e.getMessage()).contains("A network error")) {
                            Toast.makeText(
                                    AuthorizationActivity.this,
                                    "Ошибка авторизации: потеряно соединение с интернетом",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                        else {
                            Toast.makeText(
                                    AuthorizationActivity.this,
                                    "Ошибка авторизации: " + e.getMessage(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
        });

        buttonRestorePassword.setOnClickListener(view -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
            finishAffinity();
            finish();
        });

        Button buttonRegistration = findViewById(R.id.buttonRegistration);
        /*
          Привязка к кнопке функции перехода в активность регистрации
         */
        buttonRegistration.setOnClickListener(view -> {
            startActivity(new Intent(this, RegistrationActivity.class));
            finishAffinity();
            finish();
        });
    }
}

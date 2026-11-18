package com.eddyizm.tempus.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;

import com.eddyizm.tempus.App;
import com.eddyizm.tempus.R;
import com.eddyizm.tempus.databinding.FragmentLoginBinding;
import com.eddyizm.tempus.interfaces.SystemCallback;
import com.eddyizm.tempus.model.Server;
import com.eddyizm.tempus.repository.PlaylistRepository;
import com.eddyizm.tempus.repository.SystemRepository;
import com.eddyizm.tempus.ui.activity.MainActivity;
import com.eddyizm.tempus.util.Preferences;
import com.eddyizm.tempus.viewmodel.LoginViewModel;

@UnstableApi
public class LoginFragment extends Fragment {
    private static final String DEFAULT_SERVER_ID = "poordooytify_default_server";

    private FragmentLoginBinding bind;
    private MainActivity activity;
    private LoginViewModel loginViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        loginViewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);
        bind = FragmentLoginBinding.inflate(inflater, container, false);

        applySystemInsets();
        prefillUsername();
        setupActions();

        return bind.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    private void applySystemInsets() {
        final int baseTop = bind.loginContainer.getPaddingTop();
        final int baseBottom = bind.loginContainer.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(bind.loginContainer, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), baseTop + insets.top, v.getPaddingRight(), baseBottom + insets.bottom);
            return windowInsets;
        });
    }

    private void prefillUsername() {
        String savedUser = Preferences.getUser();
        if (!TextUtils.isEmpty(savedUser)) {
            bind.usernameField.setText(savedUser);
        }
    }

    private void setupActions() {
        bind.passwordField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        bind.signInButton.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        if (bind == null) return;

        String username = bind.usernameField.getText() != null
                ? bind.usernameField.getText().toString().trim()
                : "";
        String password = bind.passwordField.getText() != null
                ? bind.passwordField.getText().toString()
                : "";

        bind.usernameFieldContainer.setError(null);
        bind.passwordFieldContainer.setError(null);

        if (TextUtils.isEmpty(username)) {
            bind.usernameFieldContainer.setError(getString(R.string.login_missing_credentials));
            bind.usernameField.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            bind.passwordFieldContainer.setError(getString(R.string.login_missing_credentials));
            bind.passwordField.requestFocus();
            return;
        }

        String serverUrl = getString(R.string.default_server_url);
        setLoading(true);
        saveServerPreference(serverUrl, username, password);

        SystemRepository systemRepository = new SystemRepository();
        systemRepository.checkUserCredential(new SystemCallback() {
            @Override
            public void onError(Exception exception) {
                resetServerPreference();
                if (bind != null) {
                    setLoading(false);
                }

                Context context = getContext();
                if (context != null && isAdded()) {
                    Toast.makeText(context, exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onSuccess(String resolvedPassword, String token, String salt) {
                loginViewModel.addServer(new Server(
                        DEFAULT_SERVER_ID,
                        getString(R.string.app_name),
                        username,
                        resolvedPassword != null ? resolvedPassword : password,
                        serverUrl,
                        null,
                        System.currentTimeMillis(),
                        false,
                        null,
                        null
                ));
                new PlaylistRepository().refreshAllPlaylists();
                if (activity != null && isAdded()) {
                    activity.goFromLogin();
                }
            }
        });
    }

    private void setLoading(boolean loading) {
        bind.signInButton.setEnabled(!loading);
        bind.signInButton.setText(loading ? R.string.login_signing_in : R.string.login_sign_in_button);
        bind.usernameField.setEnabled(!loading);
        bind.passwordField.setEnabled(!loading);
    }

    private void saveServerPreference(String serverUrl, String user, String password) {
        Preferences.setServerId(DEFAULT_SERVER_ID);
        Preferences.setServer(serverUrl);
        Preferences.setLocalAddress(null);
        Preferences.setInUseServerAddress(serverUrl);
        Preferences.setUser(user);
        Preferences.setPassword(password);
        Preferences.setLowSecurity(false);
        Preferences.setClientCert(null);
        Preferences.setCustomHeaders(null);

        App.getSubsonicClientInstance(true);
    }

    private void resetServerPreference() {
        Preferences.setServerId(null);
        Preferences.setServer(null);
        Preferences.setUser(null);
        Preferences.setPassword(null);
        Preferences.setToken(null);
        Preferences.setSalt(null);
        Preferences.setLowSecurity(false);
        Preferences.setClientCert(null);
        Preferences.setCustomHeaders(null);

        App.getSubsonicClientInstance(true);
    }
}

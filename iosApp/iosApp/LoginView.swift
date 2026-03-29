import SwiftUI

struct LoginView: View {
    let onAuthSuccess: () -> Void
    let onForgotPasswordTap: () -> Void

    @StateObject private var viewModel = AuthViewModel()
    @State private var email = ""
    @State private var password = ""
    @State private var passwordVisible = false

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)
    private let backgroundGray = Color(red: 0.949, green: 0.957, blue: 0.973)
    private let fieldBackground = Color(red: 0.973, green: 0.976, blue: 0.98)
    private let borderColor = Color(red: 0.878, green: 0.878, blue: 0.878)

    private var isLoading: Bool {
        if case .loading = viewModel.uiState { return true }
        return false
    }

    private var errorMessage: String? {
        if case .error(let msg) = viewModel.uiState { return msg }
        return nil
    }

    var body: some View {
        ZStack {
            backgroundGray.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 16) {
                    Spacer().frame(height: 48)

                    // Header card
                    VStack(spacing: 0) {
                        RoundedRectangle(cornerRadius: 16)
                            .fill(navyBlue)
                            .frame(width: 72, height: 72)
                            .overlay {
                                Image(systemName: "building.2")
                                    .font(.system(size: 32, weight: .regular))
                                    .foregroundColor(.white)
                            }
                        Spacer().frame(height: 16)
                        Text("Welcome to Indsphinx")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                        Spacer().frame(height: 4)
                        Text("Housing Management System")
                            .font(.system(size: 14))
                            .foregroundColor(.gray)
                    }
                    .padding(.vertical, 28)
                    .padding(.horizontal, 24)
                    .frame(maxWidth: .infinity)
                    .background(Color.white)
                    .cornerRadius(20)
                    .shadow(color: .black.opacity(0.06), radius: 8, x: 0, y: 2)

                    // Form card
                    VStack(alignment: .leading, spacing: 0) {
                        Text("Employee ID / Email")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(Color(red: 0.2, green: 0.2, blue: 0.2))
                        Spacer().frame(height: 8)
                        HStack(spacing: 10) {
                            Image(systemName: "envelope").foregroundColor(.gray).frame(width: 20)
                            TextField("Enter your employee ID or email", text: $email)
                                .autocorrectionDisabled()
                                .keyboardType(.emailAddress)
                                .onChange(of: email, perform: { _ in viewModel.resetState() })
                        }
                        .padding(14)
                        .background(fieldBackground)
                        .cornerRadius(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(borderColor, lineWidth: 1))

                        Spacer().frame(height: 16)

                        Text("Password")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(Color(red: 0.2, green: 0.2, blue: 0.2))
                        Spacer().frame(height: 8)
                        HStack(spacing: 10) {
                            Image(systemName: "lock").foregroundColor(.gray).frame(width: 20)
                            if passwordVisible {
                                TextField("Enter your password", text: $password)
                                    .autocorrectionDisabled()
                            } else {
                                SecureField("Enter your password", text: $password)
                            }
                            Button(action: { passwordVisible.toggle() }) {
                                Image(systemName: passwordVisible ? "eye.slash" : "eye").foregroundColor(.gray)
                            }
                        }
                        .padding(14)
                        .background(fieldBackground)
                        .cornerRadius(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(borderColor, lineWidth: 1))

                        HStack {
                            Spacer()
                            Button(action: onForgotPasswordTap) {
                                Text("Forgot Password?")
                                    .font(.system(size: 14, weight: .medium))
                                    .foregroundColor(navyBlue)
                            }
                        }
                        .padding(.top, 10)
                        .padding(.bottom, 6)

                        if let error = errorMessage {
                            Text(error)
                                .font(.system(size: 13))
                                .foregroundColor(Color(red: 0.898, green: 0.224, blue: 0.208))
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.bottom, 8)
                        }

                        Button(action: {
                            viewModel.signIn(email: email.trimmingCharacters(in: .whitespaces), password: password)
                        }) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 12).fill(navyBlue)
                                if isLoading {
                                    ProgressView().progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    HStack(spacing: 8) {
                                        Text("Login")
                                            .font(.system(size: 16, weight: .semibold))
                                        Image(systemName: "arrow.right")
                                            .font(.system(size: 14, weight: .semibold))
                                    }
                                    .foregroundColor(.white)
                                }
                            }
                            .frame(maxWidth: .infinity, minHeight: 52, maxHeight: 52)
                        }
                        .disabled(isLoading)
                    }
                    .padding(24)
                    .background(Color.white)
                    .cornerRadius(20)
                    .shadow(color: .black.opacity(0.06), radius: 8, x: 0, y: 2)

                    Spacer().frame(height: 24)

                    VStack(spacing: 4) {
                        Text("Secure login powered by Indsphinx").font(.system(size: 12)).foregroundColor(.gray)
                        Text("© 2026 Indsphinx Accommodation System").font(.system(size: 12)).foregroundColor(.gray)
                    }

                    Spacer().frame(height: 48)
                }
                .padding(.horizontal, 20)
            }
        }
        .onChange(of: viewModel.uiState, perform: { newState in
            if case .success = newState {
                onAuthSuccess()
            }
        })
    }
}

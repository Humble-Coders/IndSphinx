import SwiftUI

struct HomeView: View {
    let onSignOut: () -> Void

    @StateObject private var viewModel = HomeViewModel()

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)
    private let backgroundGray = Color(red: 0.949, green: 0.957, blue: 0.973)

    var body: some View {
        ZStack {
            backgroundGray.ignoresSafeArea()

            VStack(spacing: 0) {
                RoundedRectangle(cornerRadius: 22)
                    .fill(navyBlue)
                    .frame(width: 88, height: 88)
                    .overlay {
                        Image(systemName: "building.2")
                            .font(.system(size: 44, weight: .regular))
                            .foregroundColor(.white)
                    }

                Spacer().frame(height: 24)

                Text("Welcome Back!")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))

                if !viewModel.currentEmail.isEmpty {
                    Spacer().frame(height: 8)
                    Text(viewModel.currentEmail)
                        .font(.system(size: 15))
                        .foregroundColor(.gray)
                }

                Spacer().frame(height: 8)

                Text("You're successfully signed in to\nIndsphinx Accommodation System")
                    .font(.system(size: 13))
                    .foregroundColor(Color(red: 0.6, green: 0.6, blue: 0.6))
                    .multilineTextAlignment(.center)

                Spacer().frame(height: 40)

                Button(action: {
                    viewModel.signOut()
                    onSignOut()
                }) {
                    HStack(spacing: 8) {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                            .font(.system(size: 16))
                        Text("Sign Out")
                            .font(.system(size: 16, weight: .medium))
                    }
                    .foregroundColor(Color(red: 0.4, green: 0.4, blue: 0.4))
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color(red: 0.96, green: 0.96, blue: 0.96))
                    .cornerRadius(12)
                }
            }
            .padding(.horizontal, 32)
            .padding(.vertical, 48)
            .frame(maxWidth: .infinity)
            .background(Color.white)
            .cornerRadius(24)
            .shadow(color: .black.opacity(0.08), radius: 12, x: 0, y: 4)
            .padding(.horizontal, 24)
        }
    }
}

import SwiftUI

/// The occupant's own assets: what they hold now, and what they held before.
///
/// Read-only. Assets are issued and taken back from the admin portal; this
/// screen just reflects the live record.
struct AssetsView: View {
    let authUid: String
    let onBack: () -> Void

    @StateObject private var viewModel = AssetsViewModel()

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)
    private let background = Color(red: 0.949, green: 0.957, blue: 0.973)
    private let muted = Color(red: 0.42, green: 0.447, blue: 0.502)

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "xmark")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(.white)
                            .padding(8)
                    }
                    Spacer()
                }
                .padding(.horizontal, 8)
                Text("My Assets")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.white)
            }
            .frame(height: 52)
            .background {
                navyBlue.ignoresSafeArea(edges: .top)
            }

            switch viewModel.state {
            case .loading:
                Spacer()
                ProgressView().tint(navyBlue)
                Spacer()

            case .error(let message):
                Spacer()
                Text(message)
                    .font(.system(size: 15))
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
                    .padding(24)
                Spacer()

            case .loaded(let snapshot):
                if snapshot.isEmpty {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "shippingbox")
                            .font(.system(size: 44))
                            .foregroundColor(Color(red: 0.78, green: 0.796, blue: 0.831))
                        Text("No assets assigned to you yet.")
                            .font(.system(size: 15))
                            .foregroundColor(muted)
                    }
                    Spacer()
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 10) {
                            if !snapshot.current.isEmpty {
                                sectionHeader("Currently Held", snapshot.current.count)
                                ForEach(snapshot.current) { asset in
                                    AssetCardView(asset: asset)
                                }
                            }
                            if !snapshot.past.isEmpty {
                                sectionHeader("Previously Held", snapshot.past.count)
                                    .padding(.top, snapshot.current.isEmpty ? 0 : 6)
                                ForEach(snapshot.past) { asset in
                                    AssetCardView(asset: asset)
                                }
                            }
                        }
                        .padding(16)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(background)
        .onAppear { viewModel.start(authUid: authUid) }
        .onChange(of: authUid) { newValue in viewModel.start(authUid: newValue) }
    }

    private func sectionHeader(_ title: String, _ count: Int) -> some View {
        Text("\(title) (\(count))")
            .font(.system(size: 12, weight: .bold))
            .foregroundColor(muted)
    }
}

private struct AssetCardView: View {
    let asset: OccupantAsset

    private let muted = Color(red: 0.42, green: 0.447, blue: 0.502)
    private let border = Color(red: 0.898, green: 0.906, blue: 0.922)

    /// Assigned reads as active; the closed states as neutral, warning, error.
    private var chipColors: (bg: Color, fg: Color) {
        switch asset.status.uppercased() {
        case OccupantAsset.statusReturned:
            return (Color(red: 0.906, green: 0.965, blue: 0.937), Color(red: 0.059, green: 0.482, blue: 0.310))
        case OccupantAsset.statusDamaged:
            return (Color(red: 0.992, green: 0.953, blue: 0.886), Color(red: 0.604, green: 0.384, blue: 0.027))
        case OccupantAsset.statusMissing:
            return (Color(red: 0.992, green: 0.925, blue: 0.925), Color(red: 0.702, green: 0.145, blue: 0.118))
        default:
            return (Color(red: 0.910, green: 0.925, blue: 0.969), Color(red: 0.118, green: 0.176, blue: 0.42))
        }
    }

    private func formatted(_ date: Date?) -> String {
        guard let date else { return "-" }
        let f = DateFormatter()
        f.dateFormat = "dd-MM-yyyy"
        return f.string(from: date)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(asset.assetName.isEmpty ? "Asset" : asset.assetName)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(Color(red: 0.067, green: 0.094, blue: 0.153))
                    Text("No. \(asset.assetNumber)")
                        .font(.system(size: 12))
                        .foregroundColor(muted)
                }
                Spacer()
                Text(asset.statusLabel)
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(chipColors.fg)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Capsule().fill(chipColors.bg))
            }

            Divider()
                .background(border)
                .padding(.vertical, 10)

            HStack(alignment: .top) {
                dateColumn("Assigned on", formatted(asset.assignedDate))
                if !asset.isOpen {
                    dateColumn(
                        asset.status.uppercased() == OccupantAsset.statusMissing ? "Reported on" : "Returned on",
                        formatted(asset.returnedDate)
                    )
                }
                Spacer()
            }

            // Remarks are the handover note while the item is with the
            // occupant. Once closed the field holds the admin's closing note,
            // which is not surfaced in the app.
            if asset.isOpen && !asset.remarks.trimmingCharacters(in: .whitespaces).isEmpty {
                Text(asset.remarks)
                    .font(.system(size: 12))
                    .foregroundColor(Color(red: 0.294, green: 0.333, blue: 0.388))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: 8)
                            .fill(Color(red: 0.949, green: 0.957, blue: 0.973))
                    )
                    .padding(.top, 10)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 14).fill(Color.white))
    }

    private func dateColumn(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.system(size: 11))
                .foregroundColor(muted)
            Text(value)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(Color(red: 0.067, green: 0.094, blue: 0.153))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

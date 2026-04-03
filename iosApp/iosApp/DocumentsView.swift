import SwiftUI
import WebKit

struct DocumentsView: View {
    let onBack: () -> Void
    @StateObject private var viewModel = DocumentsViewModel()
    @State private var selectedDoc: AppDocument? = nil

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)

    var body: some View {
        Group {
            if let doc = selectedDoc {
                DocumentDetailView(document: doc, onBack: { selectedDoc = nil })
            } else {
                listView
            }
        }
    }

    private var listView: some View {
        VStack(spacing: 0) {
            // Top bar
            ZStack {
                navyBlue
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
                Text("Documents")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.white)
            }
            .frame(height: 52)
            .ignoresSafeArea(edges: .top)

            switch viewModel.state {
            case .loading:
                Spacer()
                ProgressView().tint(navyBlue)
                Spacer()
            case .error(let msg):
                Spacer()
                Text(msg).foregroundColor(.red).padding()
                Spacer()
            case .ready(let docs):
                if docs.isEmpty {
                    Spacer()
                    Text("No documents available.")
                        .foregroundColor(Color(white: 0.6))
                    Spacer()
                } else {
                    List(docs) { doc in
                        Button(action: { selectedDoc = doc }) {
                            HStack(spacing: 14) {
                                ZStack {
                                    RoundedRectangle(cornerRadius: 10)
                                        .fill(Color(red: 0.933, green: 0.941, blue: 0.98))
                                        .frame(width: 44, height: 44)
                                    Image(systemName: "doc.text")
                                        .font(.system(size: 20))
                                        .foregroundColor(navyBlue)
                                }
                                Text(doc.name.isEmpty ? "Untitled" : doc.name)
                                    .font(.system(size: 15, weight: .medium))
                                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.18))
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 13))
                                    .foregroundColor(Color(white: 0.73))
                            }
                            .padding(.vertical, 6)
                        }
                        .buttonStyle(.plain)
                        .listRowBackground(Color.white)
                    }
                    .listStyle(.plain)
                    .background(Color(red: 0.949, green: 0.957, blue: 0.973))
                }
            }
        }
        .background(Color(red: 0.949, green: 0.957, blue: 0.973))
        .ignoresSafeArea(edges: .top)
    }
}

struct DocumentDetailView: View {
    let document: AppDocument
    let onBack: () -> Void

    private let navyBlue = Color(red: 0.118, green: 0.176, blue: 0.42)

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                navyBlue
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
                Text(document.name.isEmpty ? "Document" : document.name)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.white)
                    .lineLimit(1)
                    .padding(.horizontal, 52)
            }
            .frame(height: 52)
            .ignoresSafeArea(edges: .top)

            HTMLView(html: document.htmlContent)
        }
        .ignoresSafeArea(edges: .top)
    }
}

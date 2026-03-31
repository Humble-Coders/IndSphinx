import Foundation
import FirebaseStorage

class BackendStorageRepository {
    private let storage = Storage.storage()

    func uploadData(_ data: Data, path: String) async throws -> String {
        let ref = storage.reference().child(path)
        return try await withCheckedThrowingContinuation { continuation in
            ref.putData(data, metadata: nil) { _, error in
                if let error = error {
                    continuation.resume(throwing: error)
                    return
                }
                ref.downloadURL { url, error in
                    if let url = url {
                        continuation.resume(returning: url.absoluteString)
                    } else {
                        continuation.resume(throwing: error ?? NSError(
                            domain: "BackendStorageRepository",
                            code: -1,
                            userInfo: [NSLocalizedDescriptionKey: "Failed to get download URL"]
                        ))
                    }
                }
            }
        }
    }

    func uploadFile(from localURL: URL, path: String) async throws -> String {
        let ref = storage.reference().child(path)
        return try await withCheckedThrowingContinuation { continuation in
            ref.putFile(from: localURL, metadata: nil) { _, error in
                if let error = error {
                    continuation.resume(throwing: error)
                    return
                }
                ref.downloadURL { url, error in
                    if let url = url {
                        continuation.resume(returning: url.absoluteString)
                    } else {
                        continuation.resume(throwing: error ?? NSError(
                            domain: "BackendStorageRepository",
                            code: -1,
                            userInfo: [NSLocalizedDescriptionKey: "Failed to get download URL"]
                        ))
                    }
                }
            }
        }
    }
}

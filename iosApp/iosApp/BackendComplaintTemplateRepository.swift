import Foundation
import FirebaseFirestore

struct ComplaintTemplate: Hashable {
    let category: String
    let problems: [String]
    /// Admin-decided default priority, keyed by PROBLEM name — not by category.
    /// "Making Noise" carries a priority; "AC" does not.
    ///
    /// Optional by design. Templates created before this feature have no map at
    /// all, and those problems keep letting the occupant choose. Stored in
    /// Firestore as a separate `problemPriorities` map so the `problems` array
    /// older app builds read stays unchanged.
    let problemPriorities: [String: String]

    /// Explicit init with a default so existing call sites that construct a
    /// template with only category + problems keep compiling.
    init(category: String, problems: [String], problemPriorities: [String: String] = [:]) {
        self.category = category
        self.problems = problems
        self.problemPriorities = problemPriorities
    }

    /// The only priorities this build can display and submit.
    static let priorities = ["Low", "Medium", "High", "Emergency"]

    /// Maps a stored value onto its canonical casing, or nil if unknown.
    static func canonicalPriority(_ value: String?) -> String? {
        guard let trimmed = value?.trimmingCharacters(in: .whitespacesAndNewlines),
              !trimmed.isEmpty else { return nil }
        return priorities.first { $0.caseInsensitiveCompare(trimmed) == .orderedSame }
    }

    /// The admin-decided priority for `problem`, or nil when the occupant is
    /// free to pick it themselves.
    ///
    /// Returns nil — meaning "occupant chooses" — for every uncertain case: a
    /// blank problem, no entry, or a value this build does not recognise. That
    /// last one matters: if a fifth priority level is ever added on the admin
    /// side, this build falls back to the normal picker instead of locking the
    /// form to a value it cannot render or submit.
    func priority(for problem: String) -> String? {
        let wanted = problem.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !wanted.isEmpty else { return nil }

        if let exact = problemPriorities[problem] {
            return Self.canonicalPriority(exact)
        }
        // Tolerate casing / padding drift between the array entry and the key.
        for (key, value) in problemPriorities
        where key.trimmingCharacters(in: .whitespacesAndNewlines)
            .caseInsensitiveCompare(wanted) == .orderedSame {
            return Self.canonicalPriority(value)
        }
        return nil
    }
}

class BackendComplaintTemplateRepository {
    private let db = Firestore.firestore()

    /// Reads the optional `problemPriorities` map (problem name -> priority).
    ///
    /// Defensive on every axis: a missing field, a non-map value, non-string
    /// values, and unrecognised priorities are all dropped rather than
    /// propagated. Anything dropped simply falls back to the occupant choosing,
    /// which is the pre-existing behaviour.
    private static func parseProblemPriorities(_ raw: Any?) -> [String: String] {
        guard let map = raw as? [String: Any] else { return [:] }
        var out: [String: String] = [:]
        for (key, value) in map {
            guard !key.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                  let canonical = ComplaintTemplate.canonicalPriority(value as? String)
            else { continue }
            out[key] = canonical
        }
        return out
    }

    private static func makeTemplate(from doc: DocumentSnapshot) -> ComplaintTemplate? {
        let data = doc.data() ?? [:]
        guard let category = data["category"] as? String else { return nil }
        let problems = data["problems"] as? [String] ?? []
        return ComplaintTemplate(
            category: category,
            problems: problems,
            problemPriorities: parseProblemPriorities(data["problemPriorities"])
        )
    }

    func getTemplates() async throws -> [ComplaintTemplate] {
        let snapshot = try await db.collection("Templates").getDocuments()
        return snapshot.documents
            .compactMap { Self.makeTemplate(from: $0) }
            .sorted { $0.category < $1.category }
    }

    func observeTemplates(onChange: @escaping ([ComplaintTemplate]) -> Void) -> ListenerRegistration {
        return db.collection("Templates")
            .addSnapshotListener { snapshot, _ in
                guard let snapshot = snapshot else { onChange([]); return }
                let templates = snapshot.documents
                    .compactMap { Self.makeTemplate(from: $0) }
                    .sorted { $0.category < $1.category }
                onChange(templates)
            }
    }
}

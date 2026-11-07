(function () {
  "use strict";

  var DATA_KEY = "edurank.data.v1";
  var SESSION_KEY = "edurank.session";

  var state = {
    view: "dashboard",
    studentsQuery: "",
    marksQuery: "",
    rankQuery: ""
  };

  var db = null;
  var currentUser = null;
  var toastTimer = null;

  var PAGE_INFO = {
    dashboard: ["Dashboard", "Overview of students, subjects and top performers."],
    students: ["Students", "Register students and review their academic summary."],
    marks: ["Marks", "Record or update subject marks for a student."],
    rank: ["Rank List", "Look up any register number and see the live ranking."],
    faculty: ["Faculty", "Manage admin and faculty login accounts."]
  };

  function $(selector, root) {
    return (root || document).querySelector(selector);
  }

  function $$(selector, root) {
    return Array.prototype.slice.call((root || document).querySelectorAll(selector));
  }

  function content() {
    return document.getElementById("content");
  }

  function esc(value) {
    return String(value == null ? "" : value).replace(/[&<>"']/g, function (ch) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[ch];
    });
  }

  function round2(value) {
    return Math.round(value * 100) / 100;
  }

  function formatMark(value) {
    var number = Number(value);
    if (Math.abs(number - Math.round(number)) < 1e-9) {
      return String(Math.round(number));
    }
    return number.toFixed(2);
  }

  function roleLabel(role) {
    return role === "ADMIN" ? "Admin" : "Faculty";
  }

  function normReg(value) {
    return String(value == null ? "" : value).trim().toUpperCase();
  }

  function sortedStudents() {
    return db.students.slice().sort(function (a, b) {
      return a.regNo.localeCompare(b.regNo, undefined, { sensitivity: "base" });
    });
  }

  function findStudent(regNo) {
    var normalized = String(regNo == null ? "" : regNo).trim().toLowerCase();
    if (!normalized) {
      return null;
    }
    return db.students.find(function (student) {
      return student.regNo.toLowerCase() === normalized;
    }) || null;
  }

  function marksFor(regNo) {
    var normalized = String(regNo == null ? "" : regNo).trim().toLowerCase();
    return db.marks
      .filter(function (mark) {
        return mark.regNo.toLowerCase() === normalized;
      })
      .sort(function (a, b) {
        return a.subject.localeCompare(b.subject, undefined, { sensitivity: "base" });
      });
  }

  function getSubjects() {
    var seen = {};
    var subjects = [];
    db.marks.forEach(function (mark) {
      var key = mark.subject.toLowerCase();
      if (!seen[key]) {
        seen[key] = true;
        subjects.push(mark.subject);
      }
    });
    return subjects.sort(function (a, b) {
      return a.localeCompare(b, undefined, { sensitivity: "base" });
    });
  }

  function overallRankList() {
    var rows = [];
    db.students.forEach(function (student) {
      var studentMarks = marksFor(student.regNo);
      if (!studentMarks.length) {
        return;
      }
      var total = 0;
      studentMarks.forEach(function (mark) {
        total += mark.value;
      });
      rows.push({
        regNo: student.regNo,
        name: student.name,
        subjects: studentMarks.length,
        total: round2(total),
        average: round2(total / studentMarks.length)
      });
    });

    rows.sort(function (a, b) {
      return (
        b.average - a.average ||
        a.name.localeCompare(b.name, undefined, { sensitivity: "base" }) ||
        a.regNo.localeCompare(b.regNo, undefined, { sensitivity: "base" })
      );
    });

    var rank = 0;
    var previous = null;
    rows.forEach(function (row, index) {
      if (index === 0 || Math.abs(row.average - previous) > 1e-9) {
        rank = index + 1;
      }
      previous = row.average;
      row.rank = rank;
    });
    return rows;
  }

  function subjectRank(subject, regNo) {
    var target = subject.toLowerCase();
    var scoring = db.marks
      .filter(function (mark) {
        return mark.subject.toLowerCase() === target;
      })
      .sort(function (a, b) {
        return (
          b.value - a.value ||
          a.regNo.localeCompare(b.regNo, undefined, { sensitivity: "base" })
        );
      });

    var rank = 0;
    var previous = null;
    for (var i = 0; i < scoring.length; i++) {
      if (i === 0 || Math.abs(scoring[i].value - previous) > 1e-9) {
        rank = i + 1;
      }
      if (scoring[i].regNo.toLowerCase() === regNo.toLowerCase()) {
        return {
          subject: scoring[i].subject,
          mark: scoring[i].value,
          rank: rank,
          total: scoring.length
        };
      }
      previous = scoring[i].value;
    }
    return { subject: subject, mark: 0, rank: 0, total: scoring.length };
  }

  function buildReport(regNo) {
    var student = findStudent(regNo);
    if (!student) {
      return null;
    }
    var studentMarks = marksFor(student.regNo);
    var total = 0;
    var subjectRanks = studentMarks.map(function (mark) {
      total += mark.value;
      return subjectRank(mark.subject, student.regNo);
    });
    subjectRanks.sort(function (a, b) {
      return a.subject.localeCompare(b.subject, undefined, { sensitivity: "base" });
    });

    var overall = overallRankList();
    var row = overall.find(function (item) {
      return item.regNo === student.regNo;
    });

    return {
      student: student,
      subjectRanks: subjectRanks,
      average: studentMarks.length ? round2(total / studentMarks.length) : 0,
      overallRank: row ? row.rank : 0,
      classSize: overall.length
    };
  }

  function subjectStats() {
    var grouped = {};
    db.marks.forEach(function (mark) {
      var key = mark.subject.toLowerCase();
      if (!grouped[key]) {
        grouped[key] = { subject: mark.subject, total: 0, count: 0 };
      }
      grouped[key].total += mark.value;
      grouped[key].count += 1;
    });
    return Object.keys(grouped)
      .map(function (key) {
        var item = grouped[key];
        return {
          subject: item.subject,
          average: round2(item.total / item.count),
          students: item.count
        };
      })
      .sort(function (a, b) {
        return a.subject.localeCompare(b.subject, undefined, { sensitivity: "base" });
      });
  }

  function saveDb() {
    try {
      localStorage.setItem(DATA_KEY, JSON.stringify(db));
    } catch (error) {
      toast("Could not save data in this browser.", "error");
    }
  }

  function loadDb() {
    try {
      var raw = localStorage.getItem(DATA_KEY);
      if (!raw) {
        return null;
      }
      var parsed = JSON.parse(raw);
      if (
        !parsed ||
        !Array.isArray(parsed.users) ||
        !Array.isArray(parsed.students) ||
        !Array.isArray(parsed.marks)
      ) {
        return null;
      }
      return parsed;
    } catch (error) {
      return null;
    }
  }

  function toast(message, type) {
    var element = document.getElementById("toast");
    if (!element) {
      return;
    }
    element.textContent = message;
    element.className = "toast show" + (type === "error" ? " error" : "");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      element.className = "toast" + (type === "error" ? " error" : "");
    }, 3200);
  }

  function setStatus(id, message, type) {
    var element = document.getElementById(id);
    if (!element) {
      return;
    }
    element.textContent = message || "";
    element.className = "status" + (type ? " " + type : "");
  }

  function randomHex(bytes) {
    var values = new Uint8Array(bytes);
    crypto.getRandomValues(values);
    return Array.prototype.map
      .call(values, function (value) {
        return value.toString(16).padStart(2, "0");
      })
      .join("");
  }

  function sha256(text) {
    return crypto.subtle
      .digest("SHA-256", new TextEncoder().encode(text))
      .then(function (digest) {
        return Array.prototype.map
          .call(new Uint8Array(digest), function (value) {
            return value.toString(16).padStart(2, "0");
          })
          .join("");
      });
  }

  function makeCredential(password) {
    var salt = randomHex(16);
    return sha256(salt + password).then(function (hash) {
      return { salt: salt, hash: hash };
    });
  }

  function makeUser(username, displayName, role, password) {
    return makeCredential(password).then(function (credential) {
      return {
        username: username,
        displayName: displayName,
        role: role,
        salt: credential.salt,
        hash: credential.hash
      };
    });
  }

  async function createSeedDb() {
    var users = [
      await makeUser("admin", "System Administrator", "ADMIN", "admin123"),
      await makeUser("faculty", "Prof. Faculty", "FACULTY", "faculty123")
    ];

    var students = [
      { regNo: "21CS001", name: "Aarav Sharma" },
      { regNo: "21CS002", name: "Diya Patel" },
      { regNo: "21CS003", name: "Rohan Mehta" },
      { regNo: "21CS004", name: "Ananya Iyer" },
      { regNo: "21CS005", name: "Kabir Singh" },
      { regNo: "21CS006", name: "Meera Nair" }
    ];

    var subjects = ["Mathematics", "Data Structures", "DBMS", "Operating Systems"];
    var values = [
      [88, 92, 79, 85],
      [95, 89, 91, 94],
      [72, 78, 81, 69],
      [90, 85, 88, 92],
      [65, 70, 74, 68],
      [84, 88, 90, 86]
    ];

    var marks = [];
    students.forEach(function (student, studentIndex) {
      subjects.forEach(function (subject, subjectIndex) {
        marks.push({
          regNo: student.regNo,
          subject: subject,
          value: values[studentIndex][subjectIndex]
        });
      });
    });

    return { users: users, students: students, marks: marks };
  }

  function sessionUser() {
    var username = sessionStorage.getItem(SESSION_KEY);
    if (!username) {
      return null;
    }
    return (
      db.users.find(function (user) {
        return user.username === username;
      }) || null
    );
  }

  function showApp(user) {
    currentUser = user;

    document.getElementById("sidebar-name").textContent = user.displayName;
    document.getElementById("sidebar-meta").textContent =
      "@" + user.username + "  |  " + roleLabel(user.role);

    var badge = document.getElementById("role-badge");
    badge.textContent = user.role === "ADMIN" ? "ADMIN" : "FACULTY";
    badge.className = "role-badge " + (user.role === "ADMIN" ? "admin" : "faculty");
    document.getElementById("nav-faculty").classList.toggle("hidden", user.role !== "ADMIN");

    navigate("dashboard");
    document.getElementById("login-screen").classList.add("hidden");
    document.getElementById("app").classList.remove("hidden");
  }

  function showLogin() {
    document.getElementById("app").classList.add("hidden");
    document.getElementById("login-screen").classList.remove("hidden");
    document.getElementById("login-error").textContent = "";
    document.getElementById("login-username").focus();
  }

  function navigate(view) {
    if (view === "faculty" && currentUser.role !== "ADMIN") {
      view = "dashboard";
    }
    state.view = view;
    $$(".nav-item").forEach(function (item) {
      item.classList.toggle("active", item.dataset.view === view);
    });
    document.getElementById("page-title").textContent = PAGE_INFO[view][0];
    document.getElementById("page-subtitle").textContent = PAGE_INFO[view][1];
    renderView();
  }

  function renderView() {
    switch (state.view) {
      case "students":
        renderStudentsView();
        break;
      case "marks":
        renderMarksView();
        break;
      case "rank":
        renderRankView();
        break;
      case "faculty":
        renderFacultyView();
        break;
      default:
        renderDashboard();
    }
  }

  function statCard(label, value, tone) {
    return (
      '<div class="stat-card ' + (tone || "") + '">' +
      '<span class="stat-bar"></span>' +
      "<div><span class=\"stat-label\">" + esc(label) + "</span>" +
      '<strong class="stat-value">' + esc(value) + "</strong></div>" +
      "</div>"
    );
  }

  function renderDashboard() {
    var top = overallRankList().slice(0, 5);
    var averages = subjectStats();
    var facultyCount = db.users.filter(function (user) {
      return user.role === "FACULTY";
    }).length;

    var topRows = top.length
      ? top
          .map(function (row) {
            return (
              "<tr><td><span class=\"rank-chip\">#" + row.rank + "</span></td>" +
              '<td class="mono">' + esc(row.regNo) + "</td>" +
              "<td>" + esc(row.name) + "</td>" +
              "<td>" + formatMark(row.average) + "</td></tr>"
            );
          })
          .join("")
      : '<tr><td colspan="4" class="empty">No marks recorded yet.</td></tr>';

    var averageRows = averages.length
      ? averages
          .map(function (item) {
            return (
              "<tr><td>" + esc(item.subject) + "</td>" +
              "<td>" + formatMark(item.average) + "</td>" +
              "<td>" + item.students + "</td></tr>"
            );
          })
          .join("")
      : '<tr><td colspan="3" class="empty">No subjects recorded yet.</td></tr>';

    content().innerHTML =
      '<div class="stats-grid">' +
      statCard("STUDENTS", db.students.length, "") +
      statCard("SUBJECTS", getSubjects().length, "violet") +
      statCard("MARKS RECORDED", db.marks.length, "green") +
      statCard("FACULTY ACCOUNTS", facultyCount, "amber") +
      "</div>" +
      '<div class="grid-two">' +
      '<section class="card">' +
      '<div class="card-head"><div><h2>Top performers</h2>' +
      "<p>Students ranked by average marks across all recorded subjects.</p></div>" +
      '<button class="btn ghost small" type="button" data-view-jump="rank">View rank list</button></div>' +
      '<div class="table-wrap"><table><thead><tr><th>Rank</th><th>Reg No</th><th>Student</th><th>Average</th></tr></thead>' +
      "<tbody>" + topRows + "</tbody></table></div></section>" +
      '<section class="card">' +
      '<div class="card-head"><div><h2>Subject averages</h2>' +
      "<p>Class average for every subject recorded.</p></div></div>" +
      '<div class="table-wrap"><table><thead><tr><th>Subject</th><th>Average</th><th>Students</th></tr></thead>' +
      "<tbody>" + averageRows + "</tbody></table></div></section>" +
      "</div>";
  }

  function renderStudentsView() {
    content().innerHTML =
      '<section class="card form-card">' +
      '<form id="student-form" novalidate>' +
      '<div class="field-row">' +
      '<label class="field">Register No<input id="student-reg" placeholder="e.g. 21CS007" autocomplete="off"></label>' +
      '<label class="field grow">Student Name<input id="student-name" placeholder="Full name" autocomplete="off"></label>' +
      '<button class="btn primary" type="submit">Add student</button>' +
      '<button class="btn ghost" type="button" data-clear-student>Clear</button>' +
      "</div>" +
      '<p id="student-status" class="status"></p>' +
      "</form></section>" +
      '<section class="card">' +
      '<div class="toolbar">' +
      '<div class="toolbar-title"><h2>Student Directory</h2>' +
      "<p>Register numbers are stored in uppercase and must be unique.</p></div>" +
      '<input id="student-search" type="search" placeholder="Search register no or name" value="' +
      esc(state.studentsQuery) + '">' +
      "</div>" +
      '<div class="table-wrap"><table><thead><tr><th>Reg No</th><th>Student Name</th><th>Subjects</th><th>Average</th><th></th></tr></thead>' +
      '<tbody id="students-body"></tbody></table></div></section>';

    renderStudentsTable();
  }

  function renderStudentsTable() {
    var body = document.getElementById("students-body");
    if (!body) {
      return;
    }
    var query = state.studentsQuery.trim().toLowerCase();
    var rows = sortedStudents()
      .filter(function (student) {
        return (
          !query ||
          student.regNo.toLowerCase().indexOf(query) !== -1 ||
          student.name.toLowerCase().indexOf(query) !== -1
        );
      })
      .map(function (student) {
        var studentMarks = marksFor(student.regNo);
        var total = 0;
        studentMarks.forEach(function (mark) {
          total += mark.value;
        });
        var average = studentMarks.length
          ? formatMark(round2(total / studentMarks.length))
          : "-";
        return (
          "<tr>" +
          '<td class="mono">' + esc(student.regNo) + "</td>" +
          "<td>" + esc(student.name) + "</td>" +
          "<td>" + studentMarks.length + "</td>" +
          "<td>" + average + "</td>" +
          '<td><button class="link-danger" type="button" data-delete-student="' +
          esc(student.regNo) + '">Delete</button></td>' +
          "</tr>"
        );
      });

    body.innerHTML = rows.length
      ? rows.join("")
      : '<tr><td colspan="5" class="empty">No students found.</td></tr>';
  }

  function addStudent() {
    var regInput = document.getElementById("student-reg");
    var nameInput = document.getElementById("student-name");
    var regNo = normReg(regInput.value);
    var name = nameInput.value.trim();

    if (!regNo) {
      setStatus("student-status", "Register number is required.", "error");
      regInput.focus();
      return;
    }
    if (!/^[A-Z0-9/_-]{2,20}$/.test(regNo)) {
      setStatus("student-status", "Register number must be 2-20 characters (A-Z, 0-9, / _ -).", "error");
      regInput.focus();
      return;
    }
    if (!name) {
      setStatus("student-status", "Student name is required.", "error");
      nameInput.focus();
      return;
    }
    if (findStudent(regNo)) {
      setStatus("student-status", "Register number " + regNo + " already exists.", "error");
      regInput.focus();
      return;
    }

    db.students.push({ regNo: regNo, name: name });
    saveDb();
    regInput.value = "";
    nameInput.value = "";
    setStatus("student-status", "Student " + regNo + " added.", "success");
    toast("Student added.");
    renderStudentsTable();
    regInput.focus();
  }

  function deleteStudent(regNo) {
    var student = findStudent(regNo);
    if (!student) {
      return;
    }
    if (!confirm("Delete " + student.name + " (" + student.regNo + ") and all recorded marks?")) {
      return;
    }
    db.students = db.students.filter(function (item) {
      return item.regNo !== student.regNo;
    });
    db.marks = db.marks.filter(function (mark) {
      return mark.regNo !== student.regNo;
    });
    saveDb();
    toast("Student " + student.regNo + " deleted.");
    renderStudentsTable();
  }

  function renderMarksView() {
    var students = sortedStudents();
    var options = students.length
      ? students
          .map(function (student) {
            return (
              '<option value="' + esc(student.regNo) + '">' +
              esc(student.regNo) + " - " + esc(student.name) + "</option>"
            );
          })
          .join("")
      : '<option value="">No students registered</option>';

    var subjects = getSubjects();

    content().innerHTML =
      '<section class="card form-card">' +
      '<form id="marks-form" novalidate>' +
      '<div class="field-row">' +
      '<label class="field student-select">Student<select id="mark-student"' +
      (students.length ? "" : " disabled") + ">" + options + "</select></label>" +
      '<label class="field grow">Subject<input id="mark-subject" list="subject-options" placeholder="e.g. Mathematics" autocomplete="off"></label>' +
      '<label class="field">Mark (0-100)<input id="mark-value" type="number" min="0" max="100" step="0.01" placeholder="0 - 100"></label>' +
      '<button class="btn primary" type="submit">Save mark</button>' +
      "</div>" +
      '<p id="marks-status" class="status"></p>' +
      "</form>" +
      '<datalist id="subject-options">' +
      subjects
        .map(function (subject) {
          return '<option value="' + esc(subject) + '"></option>';
        })
        .join("") +
      "</datalist></section>" +
      '<section class="card">' +
      '<div class="toolbar">' +
      '<div class="toolbar-title"><h2>Recorded Marks</h2>' +
      "<p>Each student can have one mark per subject; saving again updates it.</p></div>" +
      '<input id="marks-search" type="search" placeholder="Search student, register no or subject" value="' +
      esc(state.marksQuery) + '">' +
      "</div>" +
      '<div class="table-wrap"><table><thead><tr><th>Reg No</th><th>Student</th><th>Subject</th><th>Mark</th><th></th></tr></thead>' +
      '<tbody id="marks-body"></tbody></table></div></section>';

    renderMarksTable();
  }

  function renderMarksTable() {
    var body = document.getElementById("marks-body");
    if (!body) {
      return;
    }
    var query = state.marksQuery.trim().toLowerCase();
    var marks = db.marks.slice().sort(function (a, b) {
      return (
        a.regNo.localeCompare(b.regNo, undefined, { sensitivity: "base" }) ||
        a.subject.localeCompare(b.subject, undefined, { sensitivity: "base" })
      );
    });

    var rows = marks
      .filter(function (mark) {
        var student = findStudent(mark.regNo);
        var name = student ? student.name.toLowerCase() : "";
        return (
          !query ||
          mark.regNo.toLowerCase().indexOf(query) !== -1 ||
          mark.subject.toLowerCase().indexOf(query) !== -1 ||
          name.indexOf(query) !== -1
        );
      })
      .map(function (mark) {
        var student = findStudent(mark.regNo);
        return (
          "<tr>" +
          '<td class="mono">' + esc(mark.regNo) + "</td>" +
          "<td>" + esc(student ? student.name : "") + "</td>" +
          "<td>" + esc(mark.subject) + "</td>" +
          "<td>" + formatMark(mark.value) + "</td>" +
          '<td><button class="link-danger" type="button" data-delete-mark data-reg="' +
          esc(mark.regNo) + '" data-subject="' + esc(mark.subject) + '">Delete</button></td>' +
          "</tr>"
        );
      });

    body.innerHTML = rows.length
      ? rows.join("")
      : '<tr><td colspan="5" class="empty">No marks found.</td></tr>';
  }

  function renderSubjectDatalist() {
    var list = document.getElementById("subject-options");
    if (!list) {
      return;
    }
    list.innerHTML = getSubjects()
      .map(function (subject) {
        return '<option value="' + esc(subject) + '"></option>';
      })
      .join("");
  }

  function saveMark() {
    var studentSelect = document.getElementById("mark-student");
    var subjectInput = document.getElementById("mark-subject");
    var markInput = document.getElementById("mark-value");

    var regNo = studentSelect ? studentSelect.value : "";
    var subject = subjectInput.value.trim();
    var raw = markInput.value.trim();

    if (!regNo) {
      setStatus("marks-status", "Add a student first, then record marks.", "error");
      return;
    }
    if (!subject) {
      setStatus("marks-status", "Subject is required.", "error");
      subjectInput.focus();
      return;
    }
    var value = Number(raw);
    if (raw === "" || !Number.isFinite(value) || value < 0 || value > 100) {
      setStatus("marks-status", "Mark must be a number between 0 and 100.", "error");
      markInput.focus();
      return;
    }

    var student = findStudent(regNo);
    if (!student) {
      setStatus("marks-status", "Student not found.", "error");
      return;
    }

    var existing = db.marks.find(function (mark) {
      return (
        mark.regNo === student.regNo &&
        mark.subject.toLowerCase() === subject.toLowerCase()
      );
    });

    if (existing) {
      existing.subject = subject;
      existing.value = value;
    } else {
      db.marks.push({ regNo: student.regNo, subject: subject, value: value });
    }

    saveDb();
    markInput.value = "";
    setStatus(
      "marks-status",
      "Saved " + subject + " = " + formatMark(value) + " for " + student.regNo + ".",
      "success"
    );
    toast("Mark saved.");
    renderSubjectDatalist();
    renderMarksTable();
    markInput.focus();
  }

  function deleteMark(regNo, subject) {
    var student = findStudent(regNo);
    if (!confirm("Delete the " + subject + " mark for " + regNo + "?")) {
      return;
    }
    db.marks = db.marks.filter(function (mark) {
      return !(
        mark.regNo.toLowerCase() === regNo.toLowerCase() &&
        mark.subject.toLowerCase() === subject.toLowerCase()
      );
    });
    saveDb();
    toast("Mark deleted.");
    renderMarksTable();
    if (student) {
      renderSubjectDatalist();
    }
  }

  function renderRankView() {
    content().innerHTML =
      '<section class="card">' +
      '<form id="rank-form" class="rank-search" novalidate>' +
      '<label class="field">Register No<input id="rank-search" type="search" placeholder="e.g. 21CS001" value="' +
      esc(state.rankQuery) + '"></label>' +
      '<button class="btn primary" type="submit">Get rank</button>' +
      '<button class="btn ghost" type="button" data-clear-rank>Clear</button>' +
      '<span class="rank-tip">Type a register number and press Enter.</span>' +
      "</form>" +
      '<p id="rank-status" class="status"></p>' +
      "</section>" +
      '<section class="card">' +
      '<div class="rank-detail-head">' +
      '<div class="rank-identity"><h2 id="rank-name">No student selected</h2>' +
      '<p id="rank-reg">Enter a register number above to view details.</p></div>' +
      '<div class="mini-stats">' +
      '<div class="mini-stat"><span>OVERALL RANK</span><strong id="rank-overall">-</strong></div>' +
      '<div class="mini-stat"><span>AVERAGE MARK</span><strong id="rank-average">-</strong></div>' +
      '<div class="mini-stat"><span>SUBJECTS</span><strong id="rank-subjects">-</strong></div>' +
      "</div></div>" +
      '<div class="table-wrap"><table><thead><tr><th>Subject</th><th>Mark</th><th>Subject Rank</th></tr></thead>' +
      '<tbody id="rank-subject-body"><tr><td colspan="3" class="empty">No student selected.</td></tr></tbody></table></div>' +
      "</section>" +
      '<section class="card">' +
      '<div class="card-head"><div><h2>Overall Rank List</h2>' +
      "<p>Competition ranking by average marks across every recorded subject. Click a row to view that student.</p></div></div>" +
      '<div class="table-wrap"><table><thead><tr><th>Rank</th><th>Reg No</th><th>Student</th><th>Subjects</th><th>Total</th><th>Average</th></tr></thead>' +
      '<tbody id="overall-rank-body"></tbody></table></div></section>';

    renderRankTable();

    if (state.rankQuery) {
      showRankReport(state.rankQuery, false);
    }
  }

  function renderRankTable() {
    var body = document.getElementById("overall-rank-body");
    if (!body) {
      return;
    }
    var rows = overallRankList();
    body.innerHTML = rows.length
      ? rows
          .map(function (row) {
            return (
              '<tr data-rank-reg="' + esc(row.regNo) + '">' +
              '<td><span class="rank-chip">#' + row.rank + "</span></td>" +
              '<td class="mono">' + esc(row.regNo) + "</td>" +
              "<td>" + esc(row.name) + "</td>" +
              "<td>" + row.subjects + "</td>" +
              "<td>" + formatMark(row.total) + "</td>" +
              "<td>" + formatMark(row.average) + "</td></tr>"
            );
          })
          .join("")
      : '<tr><td colspan="6" class="empty">No ranked students yet. Add students and marks first.</td></tr>';
  }

  function showRankReport(regNo, announce) {
    var normalized = normReg(regNo);
    var report = buildReport(normalized);
    var status = document.getElementById("rank-status");

    if (!report) {
      if (status) {
        status.className = "status error";
        status.textContent =
          "No student found with register number " + normalized + ".";
      }
      clearRankDetail();
      return;
    }

    state.rankQuery = normalized;
    var input = document.getElementById("rank-search");
    if (input) {
      input.value = normalized;
    }

    document.getElementById("rank-name").textContent = report.student.name;
    document.getElementById("rank-reg").textContent =
      "Register No: " + report.student.regNo;
    document.getElementById("rank-overall").textContent = report.overallRank
      ? "#" + report.overallRank + " of " + report.classSize
      : "Not ranked";
    document.getElementById("rank-average").textContent = report.subjectRanks.length
      ? formatMark(report.average)
      : "-";
    document.getElementById("rank-subjects").textContent = String(
      report.subjectRanks.length
    );

    var body = document.getElementById("rank-subject-body");
    body.innerHTML = report.subjectRanks.length
      ? report.subjectRanks
          .map(function (item) {
            return (
              "<tr><td>" + esc(item.subject) + "</td>" +
              "<td>" + formatMark(item.mark) + "</td>" +
              "<td>#" + item.rank + " of " + item.total + "</td></tr>"
            );
          })
          .join("")
      : '<tr><td colspan="3" class="empty">No marks recorded for this student.</td></tr>';

    if (status) {
      if (announce) {
        status.className = "status success";
        status.textContent = "Showing results for " + report.student.name + ".";
      } else {
        status.className = "status";
        status.textContent = "";
      }
    }

    $$("#overall-rank-body tr").forEach(function (row) {
      row.classList.toggle("selected", row.dataset.rankReg === report.student.regNo);
    });
  }

  function clearRankDetail() {
    var name = document.getElementById("rank-name");
    if (!name) {
      return;
    }
    name.textContent = "No student selected";
    document.getElementById("rank-reg").textContent =
      "Enter a register number above to view details.";
    document.getElementById("rank-overall").textContent = "-";
    document.getElementById("rank-average").textContent = "-";
    document.getElementById("rank-subjects").textContent = "-";
    document.getElementById("rank-subject-body").innerHTML =
      '<tr><td colspan="3" class="empty">No student selected.</td></tr>';
    $$("#overall-rank-body tr").forEach(function (row) {
      row.classList.remove("selected");
    });
  }

  function renderFacultyView() {
    content().innerHTML =
      '<section class="card form-card">' +
      '<form id="faculty-form" novalidate>' +
      '<div class="field-row">' +
      '<label class="field">Username<input id="faculty-username" placeholder="3-30 characters" autocomplete="off"></label>' +
      '<label class="field grow">Display Name<input id="faculty-display" placeholder="Full name" autocomplete="off"></label>' +
      '<label class="field">Password<input id="faculty-password" type="password" placeholder="At least 4 characters" autocomplete="new-password"></label>' +
      '<label class="field">Role<select id="faculty-role"><option value="FACULTY">Faculty</option><option value="ADMIN">Admin</option></select></label>' +
      '<button class="btn primary" type="submit">Add account</button>' +
      "</div>" +
      '<p id="faculty-status" class="status"></p>' +
      "</form></section>" +
      '<section class="card">' +
      '<div class="toolbar">' +
      '<div class="toolbar-title"><h2>Login Accounts</h2>' +
      "<p>Admins can manage accounts and data. Faculty can manage students and marks.</p></div>" +
      "</div>" +
      '<div class="table-wrap"><table><thead><tr><th>Username</th><th>Display Name</th><th>Role</th><th></th></tr></thead>' +
      '<tbody id="faculty-body"></tbody></table></div></section>';

    renderFacultyTable();
  }

  function renderFacultyTable() {
    var body = document.getElementById("faculty-body");
    if (!body) {
      return;
    }
    var users = db.users.slice().sort(function (a, b) {
      return a.username.localeCompare(b.username, undefined, { sensitivity: "base" });
    });
    body.innerHTML = users
      .map(function (user) {
        return (
          "<tr>" +
          '<td class="mono">' + esc(user.username) + "</td>" +
          "<td>" + esc(user.displayName) + "</td>" +
          "<td>" + roleLabel(user.role) + "</td>" +
          '<td><button class="link-danger" type="button" data-delete-user="' +
          esc(user.username) + '">Delete</button></td>' +
          "</tr>"
        );
      })
      .join("");
  }

  async function addFacultyAccount() {
    var usernameInput = document.getElementById("faculty-username");
    var displayInput = document.getElementById("faculty-display");
    var passwordInput = document.getElementById("faculty-password");
    var roleSelect = document.getElementById("faculty-role");

    var username = usernameInput.value.trim().toLowerCase();
    var displayName = displayInput.value.trim();
    var password = passwordInput.value;
    var role = roleSelect.value;

    if (!/^[a-z0-9._-]{3,30}$/.test(username)) {
      setStatus("faculty-status", "Username must be 3-30 characters (letters, numbers, . _ -).", "error");
      usernameInput.focus();
      return;
    }
    if (db.users.some(function (user) { return user.username === username; })) {
      setStatus("faculty-status", "That username already exists.", "error");
      usernameInput.focus();
      return;
    }
    if (!displayName) {
      setStatus("faculty-status", "Display name is required.", "error");
      displayInput.focus();
      return;
    }
    if (password.length < 4) {
      setStatus("faculty-status", "Password must be at least 4 characters.", "error");
      passwordInput.focus();
      return;
    }

    try {
      var credential = await makeCredential(password);
      db.users.push({
        username: username,
        displayName: displayName,
        role: role,
        salt: credential.salt,
        hash: credential.hash
      });
      saveDb();
      usernameInput.value = "";
      displayInput.value = "";
      passwordInput.value = "";
      setStatus("faculty-status", "Account created successfully.", "success");
      toast("Account created.");
      renderFacultyTable();
    } catch (error) {
      setStatus("faculty-status", "Could not create account: " + error.message, "error");
    }
  }

  function deleteUserAccount(username) {
    var target = db.users.find(function (user) {
      return user.username === username;
    });
    if (!target) {
      return;
    }
    if (target.username === currentUser.username) {
      setStatus("faculty-status", "You cannot delete the account you are signed in with.", "error");
      return;
    }
    if (
      target.role === "ADMIN" &&
      db.users.filter(function (user) { return user.role === "ADMIN"; }).length <= 1
    ) {
      setStatus("faculty-status", "At least one admin account must remain.", "error");
      return;
    }
    if (!confirm('Delete the ' + roleLabel(target.role) + ' account "' + target.username + '"?')) {
      return;
    }
    db.users = db.users.filter(function (user) {
      return user.username !== target.username;
    });
    saveDb();
    setStatus("faculty-status", 'Account "' + target.username + '" deleted.', "success");
    toast("Account deleted.");
    renderFacultyTable();
  }

  function logout() {
    if (!currentUser) {
      return;
    }
    if (!confirm("Log out of EduRank?")) {
      return;
    }
    sessionStorage.removeItem(SESSION_KEY);
    currentUser = null;
    document.getElementById("login-password").value = "";
    showLogin();
    toast("Logged out.");
  }

  async function onLogin(event) {
    event.preventDefault();
    var error = document.getElementById("login-error");
    var role = document.getElementById("login-role").value;
    var username = document.getElementById("login-username").value.trim().toLowerCase();
    var password = document.getElementById("login-password").value;
    var button = document.querySelector("#login-form button[type=submit]");

    error.textContent = "";

    if (!username || !password) {
      error.textContent = "Please enter both username and password.";
      return;
    }

    button.disabled = true;
    button.textContent = "Signing in...";

    try {
      var user = db.users.find(function (item) {
        return item.username === username;
      });
      var valid = false;
      if (user) {
        var hash = await sha256(user.salt + password);
        valid = hash === user.hash;
      }
      if (!user || !valid) {
        error.textContent = "Invalid username or password.";
        document.getElementById("login-password").select();
        return;
      }
      if (user.role !== role) {
        error.textContent =
          "This account is registered as " + roleLabel(user.role) + ", not " +
          roleLabel(role) + ".";
        return;
      }
      sessionStorage.setItem(SESSION_KEY, user.username);
      showApp(user);
    } catch (loginError) {
      error.textContent = "Sign-in failed: " + loginError.message;
    } finally {
      button.disabled = false;
      button.textContent = "Sign in to dashboard";
    }
  }

  function onContentSubmit(event) {
    var form = event.target;
    if (!(form instanceof HTMLFormElement)) {
      return;
    }
    event.preventDefault();

    if (form.id === "student-form") {
      addStudent();
    } else if (form.id === "marks-form") {
      saveMark();
    } else if (form.id === "rank-form") {
      var regNo = normReg(document.getElementById("rank-search").value);
      if (!regNo) {
        var status = document.getElementById("rank-status");
        status.className = "status warning";
        status.textContent = "Enter a register number first.";
        return;
      }
      state.rankQuery = regNo;
      showRankReport(regNo, true);
    } else if (form.id === "faculty-form") {
      addFacultyAccount();
    }
  }

  function onContentClick(event) {
    var target = event.target;
    if (!(target instanceof Element)) {
      return;
    }

    var jump = target.closest("[data-view-jump]");
    if (jump) {
      navigate(jump.dataset.viewJump);
      return;
    }

    var clearStudent = target.closest("[data-clear-student]");
    if (clearStudent) {
      document.getElementById("student-reg").value = "";
      document.getElementById("student-name").value = "";
      setStatus("student-status", "");
      document.getElementById("student-reg").focus();
      return;
    }

    var clearRank = target.closest("[data-clear-rank]");
    if (clearRank) {
      state.rankQuery = "";
      document.getElementById("rank-search").value = "";
      var rankStatus = document.getElementById("rank-status");
      rankStatus.className = "status";
      rankStatus.textContent = "";
      clearRankDetail();
      return;
    }

    var removeStudent = target.closest("[data-delete-student]");
    if (removeStudent) {
      deleteStudent(removeStudent.dataset.deleteStudent);
      return;
    }

    var removeMark = target.closest("[data-delete-mark]");
    if (removeMark) {
      deleteMark(removeMark.dataset.reg, removeMark.dataset.subject);
      return;
    }

    var removeUser = target.closest("[data-delete-user]");
    if (removeUser) {
      deleteUserAccount(removeUser.dataset.deleteUser);
      return;
    }

    var rankRow = target.closest("tr[data-rank-reg]");
    if (rankRow) {
      var regNo = rankRow.dataset.rankReg;
      state.rankQuery = regNo;
      showRankReport(regNo, true);
    }
  }

  function onContentInput(event) {
    var target = event.target;
    if (!(target instanceof HTMLInputElement)) {
      return;
    }
    if (target.id === "student-search") {
      state.studentsQuery = target.value;
      renderStudentsTable();
    } else if (target.id === "marks-search") {
      state.marksQuery = target.value;
      renderMarksTable();
    }
  }

  function setupEvents() {
    document.getElementById("login-form").addEventListener("submit", onLogin);
    document.getElementById("logout-btn").addEventListener("click", logout);
    $$(".nav-item").forEach(function (item) {
      item.addEventListener("click", function () {
        navigate(item.dataset.view);
      });
    });

    content().addEventListener("submit", onContentSubmit);
    content().addEventListener("click", onContentClick);
    content().addEventListener("input", onContentInput);
  }

  async function init() {
    db = loadDb();
    if (!db) {
      db = await createSeedDb();
      saveDb();
    }
    setupEvents();
    var user = sessionUser();
    if (user) {
      showApp(user);
    } else {
      showLogin();
    }
  }

  document.addEventListener("DOMContentLoaded", function () {
    init().catch(function (error) {
      var element = document.getElementById("login-error");
      if (element) {
        element.textContent = "Could not start EduRank: " + error.message;
      }
    });
  });
})();

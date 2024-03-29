;; Taken from http://github.com/bbatsov/emacs.d/raw/master/elpa-config.el
;; (which in turn was apparently taken from the Emacs Starter Kit)

;; Install a base set of packages automatically.
;;
;; Part of the Emacs Starter Kit

(defvar starter-kit-packages (list 'highlight-parentheses
                                   'javascript
                                   'js2-mode               ;; Don't know which JS mode I want yet
                                   'clojure-mode
                                   'slime
                                   'slime-repl
                                   'paredit
                                   'magit)
  "Libraries that should be installed by default.")

(defun starter-kit-elpa-install ()
  "Install all starter-kit packages that aren't installed."
  (interactive)
  ; create ELPA folder if it's not existing
  (unless (file-exists-p package-user-dir)
    (mkdir package-user-dir))
  (dolist (package starter-kit-packages)
    (unless (or (member package package-activated-list)
                (functionp package))
      (message "Installing %s" (symbol-name package))
      (package-install package))))

(defun esk-online? ()
  "See if we're online.

Windows does not have the network-interface-list function, so we
just have to assume it's online."
  ;; TODO how could this work on Windows?
  (if (and (functionp 'network-interface-list)
           (network-interface-list))
      (some (lambda (iface) (unless (equal "lo" (car iface))
                              (member 'up (first (last (network-interface-info
                                                        (car iface)))))))
            (network-interface-list))
    t))

;; On your first run, this should pull in all the base packages.
(when (esk-online?)
  (unless package-archive-contents (package-refresh-contents))
  (starter-kit-elpa-install))

(provide 'elpa-config)

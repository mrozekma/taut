package com.mrozekma.taut;

import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

public class TautFileUpload {
	// https://api.slack.com/types/file#file_types
	public enum Filetype {
		text("Plain Text", "text/plain"),
		applescript("AppleScript", "text/x-applescript"),
		boxnote("BoxNote"),
		c("C", "text/x-csrc"),
		csharp("C", "text/x-csharp"),
		cpp("C", "text/x-c++src"),
		css("CSS", "text/css"),
		csv("CSV", "text/csv"),
		clojure("Clojure", "text/x-clojure"),
		coffeescript("CoffeeScript", "text/x-coffeescript"),
		cfm("Cold Fusion", "text/x-coldfusion"),
		d("D", "text/x-d"),
		dart("Dart"),
		diff("Diff", "text/x-diff"),
		dockerfile("Docker"),
		erlang("Erlang", "text/x-erlang"),
		fsharp("F"),
		fortran("Fortran"),
		go("Go", "text/x-go"),
		groovy("Groovy", "text/x-groovy"),
		html("HTML", "text/html"),
		handlebars("Handlebars"),
		haskell("Haskell", "text/x-haskell"),
		haxe("Haxe", "text/x-haxe"),
		java("Java", "text/x-java-source"),
		javascript("JavaScript/JSON", "application/javascript"),
		kotlin("Kotlin", "application/json"),
		latex("LaTeX/sTeX", "application/x-latex"),
		lisp("Lisp", "text/x-common-lisp"),
		lua("Lua", "text/x-lua"),
		markdown("Markdown (raw)", "text/x-web-markdown"),
		matlab("MATLAB", "text/x-matlab"),
		mumps("MUMPS"),
		ocaml("OCaml", "text/x-ocaml"),
		objc("Objective-C", "text/x-objcsrc"),
		php("PHP", "text/x-php"),
		pascal("Pascal", "text/x-pascal"),
		perl("Perl", "text/x-perl"),
		pig("Pig"),
		post("Slack Post"),
		powershell("Powershell"),
		puppet("Puppet"),
		python("Python", "text/x-python"),
		r("R", "text/x-rsrc"),
		ruby("Ruby", "text/x-ruby"),
		rust("Rust"),
		sql("SQL", "text/x-sql"),
		sass("Sass"),
		scala("Scala", "text/x-scala"),
		scheme("Scheme", "text/x-scheme"),
		shell("Shell", "application/x-sh"),
		smalltalk("Smalltalk"),
		swift("Swift"),
		tsv("TSV"),
		vb("VB.NET", "text/x-vbdotnet"),
		vbscript("VBScript", "text/x-vbscript"),
		velocity("Velocity"),
		verilog("Verilog", "text/x-verilog"),
		xml("XML", "application/xml"),
		yaml("YAML", "text/x-yaml");

		final String description;
		final Optional<String> mime;

		Filetype(String description) {
			this.description = description;
			this.mime = Optional.empty();
		}

		Filetype(String description, String mime) {
			this.description = description;
			this.mime = Optional.of(mime);
		}
	}

	private byte[] data;
	private String filename;
	private Optional<String> title = Optional.empty();
	private Optional<String> initialComment = Optional.empty();
	private TautAbstractChannel[] channels = new TautAbstractChannel[0];

	// This is a string because the docs say the filetypes
	// "include, but are not limited to" the values listed in the enum
	private Optional<String> filetype = Optional.empty();

	public TautFileUpload(File file) throws IOException {
		this(Files.readAllBytes(Paths.get(file.toURI())), file.getName());
	}

	public TautFileUpload(byte[] data, String filename) {
		this.data = data;
		this.filename = filename;
	}

	public byte[] getData() { return this.data; }
	public Optional<String> getFiletype() { return this.filetype; }
	public String getFilename() { return this.filename; }
	public Optional<String> getTitle() { return this.title; }
	public Optional<String> getInitialComment() { return this.initialComment; }
	public TautAbstractChannel[] getChannels() { return this.channels; }

	public TautFileUpload setData(byte[] data) {
		this.data = data;
		return this;
	}

	public TautFileUpload setFiletype(Filetype filetype) {
		return this.setFiletype(filetype.name());
	}

	public TautFileUpload setFiletype(String filetype) {
		this.filetype = Optional.of(filetype);
		return this;
	}

	public TautFileUpload setAutoFiletype() {
		this.filetype = Optional.empty();
		return this;
	}

	public TautFileUpload setFilename(String filename) {
		this.filename = filename;
		return this;
	}

	public TautFileUpload setTitle(String title) {
		this.title = Optional.of(title);
		return this;
	}

	public TautFileUpload setInitialComment(String initialComment) {
		this.initialComment = Optional.of(initialComment);
		return this;
	}

	public TautFileUpload setChannels(TautAbstractChannel... channels) {
		this.channels = channels;
		return this;
	}

	public static TautFileUpload populateFromFile(File file) throws IOException {
		final TautFileUpload rtn = new TautFileUpload(file);
		rtn.setMimeFromTika(new Tika().detect(file));
		return rtn;
	}

	public static TautFileUpload populateFromData(byte[] data, String filename) {
		final TautFileUpload rtn = new TautFileUpload(data, filename);
		rtn.setMimeFromTika(new Tika().detect(data, filename));
		return rtn;
	}

	private void setMimeFromTika(String mime) {
		if(!mime.equals("application/octet-stream")) { // This is the default Tika returns
			Arrays.stream(Filetype.values())
					.filter(ft -> ft.mime.isPresent() && ft.mime.get().equals(mime))
					.findAny()
					.ifPresent(this::setFiletype);
		}
	}
}

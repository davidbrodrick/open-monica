use ExtUtils::MakeMaker;

WriteMakefile(
    NAME      => 'ATNF',
    VERSION   => '0.2',
    DISTNAME  => 'ATNF',
    dist      => { COMPRESS => 'gzip -9f', SUFFIX => 'gz'}
);

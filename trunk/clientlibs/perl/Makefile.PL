use ExtUtils::MakeMaker;

WriteMakefile(
    NAME      => 'ATNF',
    VERSION   => '0.8',
    DISTNAME  => 'ATNF',
    dist      => { COMPRESS => 'gzip -9f', SUFFIX => 'gz'}
);
